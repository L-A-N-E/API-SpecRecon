# SECURITY-SPRINT3.md — DevSecOps no SpecRecon

> Complementa o [`SECURITY.md`](SECURITY.md) (Sprint 2 — 5 eixos de segurança).
> Aqui estão as mudanças da **Sprint 3**, em que a segurança passou a ser um **processo contínuo**: todo commit é verificado automaticamente, achados são corrigidos ou triados, e a aplicação mede, alerta e se recupera de incidentes.
>
> Relatório completo: `Relatorio_SpecRecon_Cybersecurity_Sprint3.pdf` (entregue no Teams).

---

## 📊 Resumo

| Indicador | Resultado |
|---|---|
| Pipeline DevSecOps | GitHub Actions: **TruffleHog → Semgrep → Build + JUnit** (quality gates) |
| Achados do pipeline | Semgrep: AES/ECB (corrigido) e Actuator (triado) · TruffleHog: logs versionados (removidos) |
| Correções de segurança | **12** (tabela abaixo) |
| Testes automatizados | **23** testes de integração (8 novos nesta sprint) |
| Observabilidade | Prometheus + Grafana, **8 regras de alerta** |
| Recuperação | `backup.sh` / `restore.sh` com SHA-256 e retenção |

---

## 1. Pipeline DevSecOps — `.github/workflows/devsecops.yml`

Executa em **push na `main`, pull request e manualmente**. Cada etapa é um *quality gate*: se falhar, as seguintes não rodam.

| # | Etapa | Ferramenta | Bloqueia quando | Artefato |
|---|---|---|---|---|
| 1 | Secret Scanning | TruffleHog 3.90.8 (histórico completo) | segredo **verificado** (credencial ativa) | `trufflehog-report` |
| 2 | SAST | Semgrep (`p/java` + `p/owasp-top-ten`) | achado **WARNING** ou **ERROR** | `semgrep-report` |
| 3 | Build + Testes | Maven + JUnit 5 + MySQL 8 | build ou teste falho | `junit-reports` |

- Versões de ferramentas e actions **fixadas** (supply chain) e permissão mínima (`contents: read`).
- **SCA contínuo:** `.github/dependabot.yml` (maven, docker, github-actions — semanal).
- **Exceção triada no gate do Semgrep:** `spring-actuator-dangerous-endpoints-enabled` — só `health`/`prometheus`, na porta 9090 **não publicada** no host. O achado continua visível no relatório completo.

---

## 2. Correções de segurança

| # | Problema | Correção | Origem | Commit |
|---|---|---|---|---|
| 1 | Dados pessoais cifrados em **AES/ECB** | **AES-256-GCM** com IV sintético (HMAC-SHA256) e tag de 128 bits | Semgrep | `eff447e` |
| 2 | `POST/PUT /users` gravavam senha em **texto puro** | BCrypt nos dois endpoints | Revisão | `29b09a2` |
| 3 | Refresh token (24 h) aceito como access token | Claim `type`; filtro só aceita `access` | Revisão | `e6794b1` |
| 4 | Access token aceito em `/auth/refresh` | `/auth/refresh` exige `type=refresh` | Revisão | `e6794b1` |
| 5 | Token renovado com `role: null` | Perfil lido do banco | Revisão | `e6794b1` |
| 6 | Secret JWT com valor padrão hardcoded | **Fail-fast**: não sobe sem `JWT_SECRET` ≥ 512 bits | Revisão | `e6794b1` |
| 7 | Qualquer pessoa se registrava como **ADMIN** | Registro público só cria USER (bootstrap do 1º admin) | Revisão | `e6794b1` |
| 8 | IP lido de `X-Forwarded-For` sem validação | `ClientIpResolver` + `TRUSTED_PROXIES` | Revisão | `ea1b520` |
| 9 | Rate limit burlável, sem limite específico de login | **10 req/min em `/auth/**`**, 100 req/min no restante | Revisão | `ea1b520` |
| 10 | Logs de execução versionados | Removidos + `.gitignore` na raiz | TruffleHog | `c6f7099` |
| 11 | Spring Security em DEBUG; `/users` sem auditoria | WARN/INFO; CREATE/UPDATE/DELETE/`ROLE_CHANGED` auditados | Revisão | `ea1b520` / `29b09a2` |
| 12 | Qualquer perfil podia excluir itens do catálogo | `DELETE` restrito a **ADMIN/ANALYST** | Revisão | `18da84a` |

### Perfis (RBAC) após a Sprint 3

| Perfil | Papel equivalente | Catálogo (veículos, unidades, tipos) | `/users` |
|---|---|---|---|
| `ADMIN` | Administrador | criar, editar, **excluir** | total |
| `ANALYST` | Gestor | criar, editar, **excluir** | leitura |
| `USER` | Brigadista | criar e editar (**sem excluir**) | sem acesso |

**Registro público (`POST /auth/register`):** com o banco **vazio**, o 1º usuário pode ser ADMIN (bootstrap). Depois disso, ADMIN/ANALYST só podem ser criados por um ADMIN autenticado — tentativas retornam **403** e geram o evento `PRIVILEGE_ESCALATION_ATTEMPT`.

### Testes novos

| Classe | Prova |
|---|---|
| `JwtHardeningTest` | auto-registro como ADMIN → 403 · refresh como access → 401 · access no refresh → 401 · refresh mantém o perfil |
| `UserAuditTest` | senha criada por ADMIN em BCrypt · troca de perfil gera `ROLE_CHANGED` |
| `CatalogDeleteAuthorizationTest` | USER excluindo → 403 · ANALYST excluindo → 204 |

---

## 3. Logs, métricas e alertas

### Auditoria (`audit_log`)
Eventos novos nesta sprint: `TOKEN_REFRESH`, `PRIVILEGE_ESCALATION_ATTEMPT`, `ROLE_CHANGED`, `RATE_LIMIT_EXCEEDED` e CREATE/UPDATE/DELETE de **usuários**. IP gravado via `ClientIpResolver` (sem spoofing).

### Métricas
Todo evento de auditoria também incrementa `specrecon_security_events_total{action, status}` (Micrometer). As métricas ficam na **porta 9090, só na rede interna do Docker**; na porta pública (8443) `/actuator` continua sendo **honeypot**.

### Regras de alerta — `monitoring/prometheus/alerts.yml`

| Alerta | Gatilho | Severidade |
|---|---|---|
| `BruteForceLogin` | > 5 `FAILED_LOGIN` em 1 min | critical |
| `PrivilegeEscalationAttempt` | qualquer `PRIVILEGE_ESCALATION_ATTEMPT` | critical |
| `RoleChanged` | qualquer `ROLE_CHANGED` | warning |
| `HoneypotHit` | qualquer `HONEYPOT_HIT` | warning |
| `AccessDeniedSpike` | > 10 `UNAUTHORIZED` (403) em 5 min | warning |
| `RateLimitAbuse` | qualquer `RATE_LIMIT_EXCEEDED` | warning |
| `High5xxRate` | > 5% de respostas 5xx por 2 min | critical |
| `ApiDown` | Prometheus sem coletar por 1 min | critical |

### Como rodar o monitoramento
```bash
cd API
# adicionar ao .env:  GRAFANA_ADMIN_PASSWORD=<senha forte>
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d --build
```
- Grafana: http://localhost:3000 (usuário `admin`) — dashboard **"SpecRecon - Segurança e Observabilidade"**
- Prometheus: http://localhost:9091/alerts
- Grafana e Prometheus publicados apenas em `127.0.0.1`.

### Simular um ataque (valida os alertas)
Com a API no ar e o ADMIN já criado:
```bash
bash monitoring/simular-ataque.sh
```
Gera honeypot, 403 em série, troca de perfil, tentativa de escalada e brute force. Em ~1 min, **6 alertas** ficam em FIRING (`High5xxRate` e `ApiDown` permanecem inativos).

---

## 4. Resposta a incidentes (SANS PICERL)

| Fase | No SpecRecon |
|---|---|
| Preparação | Controles preventivos, `audit_log`, 8 alertas com *playbook*, backup verificado |
| Identificação | Alerta dispara → consulta ao `audit_log` (IP, contas-alvo, logins bem-sucedidos) |
| Contenção | Rate limit bloqueia (429); bloqueio do IP no firewall/WAF; `RATE_LIMIT_AUTH` ajustável sem deploy |
| Erradicação | Reset de senhas, revisão de `ROLE_CHANGED`, rotação do `JWT_SECRET` (invalida todos os tokens) |
| Recuperação | `restore.sh` do último backup íntegro + validação e monitoramento reforçado |

### Backup e restore — `API/scripts/`
```bash
cd API
bash scripts/backup.sh     # dump + gzip + SHA-256, mantém os 7 mais recentes
bash scripts/restore.sh    # verifica o SHA-256 e pede confirmação ("RESTAURAR")
```
Backup adulterado ou corrompido é **recusado**. A pasta `backups/` está no `.gitignore`.

---

## 5. Novas variáveis de ambiente (`.env`)

| Variável | Uso | Padrão |
|---|---|---|
| `RATE_LIMIT_AUTH` | limite/min por IP em `/auth/**` | `10` |
| `RATE_LIMIT_GENERAL` | limite/min por IP nos demais endpoints | `100` |
| `TRUSTED_PROXIES` | IPs de proxies confiáveis (vírgula) para aceitar `X-Forwarded-For` | vazio (ignora o header) |
| `GRAFANA_ADMIN_PASSWORD` | senha do Grafana (só para o monitoramento) | — obrigatória no compose de monitoramento |

> ⚠️ `JWT_SECRET` agora precisa ter **no mínimo 64 caracteres** (512 bits) — sem isso a API não inicia.
> ⚠️ Bancos criados **antes** da Sprint 3 têm e-mails no formato AES-ECB antigo. Recrie o banco: `docker compose down -v && docker compose up -d --build`.

---

## 6. Riscos aceitos

| Risco | Justificativa | Em produção |
|---|---|---|
| Chave HMAC do `X-Signature` no `test_specrecon.sh` (e no histórico do README) | Repositório **privado**; mantida para facilitar a avaliação do professor. Sozinha não dá acesso (exige JWT) | Remover do repo/histórico, nova chave em cofre de segredos com rotação |
| Logs antigos no histórico do Git | Sem senhas; reescrever histórico compartilhado na véspera da entrega traz mais risco | `git filter-repo` coordenado + rotação de credenciais |
| Actuator `health`/`prometheus` | Porta 9090 só na rede interna | Autenticação no endpoint se exposto |
| 1º registro pode ser ADMIN | Necessário para iniciar o sistema | Admin inicial via variável de ambiente |

---

## 7. Evolução planejada

- Segredos em cofre (Azure Key Vault / HashiCorp Vault) — ASVS 13.3.1
- SCA como gate no pipeline + triagem dos PRs do Dependabot — OWASP A03
- Swagger desabilitado em produção — OWASP A02 / API9
- Trivy na imagem Docker e paginação nas listagens — API4
- Alertmanager (notificação) e logs centralizados imutáveis — ASVS 16.4.2
- MFA para ADMIN — OWASP A07
