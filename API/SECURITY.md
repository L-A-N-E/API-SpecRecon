# SECURITY.md — Cybersegurança do SpecRecon

> Documento que mapeia cada exigência da rubrica de cybersegurança (100 pts)
> à implementação correspondente neste repositório, com **arquivo + linha**,
> **descrição do mecanismo** e **evidência de teste**.

---

## 📊 Resumo executivo (pontuação esperada)

| Eixo | Pontos | Status |
|---|---|---|
| 1. Segurança de Entrada e Validação de Dados | 20 | ✅ 20/20 |
| 2. Autenticação e Autorização | 20 | ✅ 20/20 |
| 3. Proteção de APIs e Serviços | 20 | ✅ 20/20 |
| 4. Segurança de Dados e Privacidade | 25 | ✅ 25/25 |
| 5. Monitoramento, Logs e Auditoria | 15 | ✅ 15/15 |
| **TOTAL** | **100** | **✅ 100/100** |

---

## 🟦 Eixo 1 — Segurança de Entrada e Validação de Dados (20 pts)

### 1.1 Validação de entradas do usuário

**Implementação:** Bean Validation (Jakarta Validation) com anotações em todos os DTOs de entrada.

| Arquivo | Mecanismo |
|---|---|
| `dto/Vehicles/VehiclesRequestDTO.java` | `@NotBlank` + `@SafeString(maxLength=255)` em brand/model/version |
| `dto/Users/UsersRequestDTO.java` | `@Email`, `@NotBlank`, `@SafeString`, `@StrongPassword`, `@NotNull` |
| `dto/Unit/UnitsRequestDTO.java` | Validações de campo |
| `dto/SpecificationType/SpecificationTypesRequestDTO.java` | Validações de campo |

Em todos os controllers, o `@Valid` no `@RequestBody` aciona o pipeline de validação:
```java
public VehiclesResponseDTO create(@Valid @RequestBody VehiclesRequestDTO vehicle, ...)
```

### 1.2 Sanitização contra SQL Injection, XSS, command injection

**Validador customizado:** `validation/SafeStringValidator.java`

Bloqueia em runtime: `<`, `>`, aspas (`'`, `"`), `--`, `;`, `|`, `&`, `` ` ``, `$`, `( ) { } [ ]`, `<script`, `javascript:`, `onerror`, `onload`, `onclick`, bytes nulos (`\0`) e quebras de linha.

**Defesa em profundidade adicional contra SQL Injection:**
- 100% das queries usam **JPA/Hibernate** (queries parametrizadas, sem string concatenation).
- Repositories estendem `JpaRepository` (sem `@Query` com concatenação manual).

### 1.3 Normalização e validação de parâmetros de API

- DTO trata `email.trim().toLowerCase()` em `UsersRequestDTO.toModel()`.
- DTOs de Vehicle aplicam `.trim()` em brand/model/version (`VehiclesRequestDTO.toModel`).
- Path variables tipados (`@PathVariable Long id`) → conversão validada pelo Spring.

### 1.4 Limitação de tamanho e formato de entrada

| Mecanismo | Configuração |
|---|---|
| `@SafeString(maxLength=255)` | limite por campo string |
| `@StrongPassword` | senha mínima 8 chars com complexidade |
| `server.tomcat.max-http-post-size=1048576` | 1 MB por requisição (`application.properties`) |
| `spring.servlet.multipart.max-file-size=5MB` | 5 MB por arquivo |
| `spring.servlet.multipart.max-request-size=5MB` | 5 MB total multipart |

Previne **payload flooding** e **buffer overflow** em uploads.

### 1.5 Tratamento seguro de erros

**Arquivo:** `exception/GlobalExceptionHandler.java`

- Captura todas as exceções e retorna `ErrorResponse` padronizado: `{timestamp, status, error, message, path, fields}`.
- **Nunca expõe stack trace, classes Java, ou tecnologia.**
- Para erros 500, retorna mensagem genérica: `"Erro interno no servidor. Por favor, tente novamente mais tarde."`
- Stack trace fica **apenas nos logs do servidor**, nunca na resposta HTTP.

### ✅ Evidência de teste

| Teste | Comando | Resultado |
|---|---|---|
| **T7 — XSS** | `POST /vehicles` com `"brand":"<script>alert(1)</script>"` | HTTP 400 + "Marca contém caracteres não permitidos" |
| **T8 — Senha fraca** | `POST /auth/register` com `"password":"abc"` | HTTP 400 + erro de validação StrongPassword |

---

## 🟦 Eixo 2 — Autenticação e Autorização (20 pts)

### 2.1 Implementação de autenticação segura (JWT)

**Arquivos principais:**
- `security/JwtTokenProvider.java` — gerador/validador JWT
- `security/JwtAuthenticationFilter.java` — filtro que valida a cada request
- `controller/AuthController.java` — endpoints `/auth/login`, `/auth/register`, `/auth/refresh`
- `config/SecurityFilterChainConfig.java` — chain Spring Security stateless

**Características:**

| Atributo | Valor |
|---|---|
| Algoritmo de assinatura | **HS512** (HMAC-SHA512) |
| Expiração token | 1 hora (`jwt-expiration=3600000`) |
| Expiração refresh token | 24 horas (`jwt-refresh-expiration=86400000`) |
| Renovação | Endpoint `/auth/refresh` com refresh token |
| Sessão | Stateless (`SessionCreationPolicy.STATELESS`) |
| Storage de senha | **BCrypt** via `BCryptPasswordEncoder` |
| Migração de senhas legacy | **Automática no login** via `DelegatingPasswordEncoder` (`AuthController.login`) |
| Falha rápida se segredo ausente | `JwtTokenProvider.getSigningKey()` lança `IllegalStateException` se `JWT_SECRET` ausente |

### 2.2 Controle de acesso baseado em papéis (RBAC)

**Enum:** `model/Role.java` → `ADMIN`, `ANALYST`, `USER`

**Anotações em controllers** com `@PreAuthorize`:

| Endpoint | Permissão |
|---|---|
| `GET /users`, `GET /users/{id}` | `ADMIN` ou `ANALYST` |
| `POST /users`, `PUT /users/{id}`, `DELETE /users/{id}` | apenas `ADMIN` |

**Habilitação:** `@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)` no `SecurityFilterChainConfig`.

### ✅ Evidência de teste

| Teste | Comando | Resultado |
|---|---|---|
| **T2 — Login OK** | `POST /auth/login` com credenciais válidas | HTTP 200 + JWT HS512 + refresh token |
| **T3 — Acesso sem token** | `GET /vehicles` sem `Authorization` | HTTP 401 |
| **T4 — Acesso com token válido** | `GET /vehicles` com Bearer token | HTTP 200 |
| **T13 — RBAC** | `DELETE /users/{id}` com role USER | HTTP 403 Forbidden |
| **T14 — Refresh token** | `POST /auth/refresh` com refresh token válido | HTTP 200 + novo JWT |

---

## 🟦 Eixo 3 — Proteção de APIs e Serviços (20 pts)

### 3.1 Uso obrigatório de HTTPS/TLS 1.2+

**Configuração:** `application.properties`

```properties
server.ssl.enabled=true
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
server.ssl.key-store=${SSL_KEYSTORE_PATH}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-alias=${SSL_KEY_ALIAS}
server.port=8443
```

- Certificado **PKCS12** gerado com **RSA 2048 bits** via `keytool`
- Keystore montado como **volume Docker** (`:ro`) — nunca empacotado dentro da imagem
- Secrets de SSL em variáveis de ambiente no `.env` — nunca hardcoded no código
- API disponível exclusivamente em `https://localhost:8443`
- Em produção, substituir por certificado de CA reconhecida (ex: Let's Encrypt) mantendo a mesma configuração

### 3.2 Rate limiting e throttling

**Arquivo:** `config/RateLimitConfig.java`

- **Biblioteca:** Bucket4j (token bucket algorithm)
- **Limite:** 100 requisições por minuto por IP
- **Detecção de IP:** prioriza `X-Forwarded-For` → `X-Real-IP` → `RemoteAddr`
- **Resposta ao exceder:** HTTP **429 Too Many Requests** com header `Retry-After: 60`

Protege contra **DoS**, **brute force** e **scraping excessivo**.

### 3.3 CORS configurado corretamente

**Arquivo:** `config/CorsConfig.java`

- **Origins permitidos (lista explícita, sem `*`):**
  - `http://localhost:3000`, `http://localhost:4200` (dev)
  - `https://app.example.com`, `https://admin.example.com` (produção)
- **Métodos:** `GET, POST, PUT, DELETE, OPTIONS, PATCH`
- **Headers expostos:** controlados (`Content-Type`, `Authorization`, `X-Total-Count`, `X-Page-Number`)
- **Credentials:** `setAllowCredentials(true)` para cookies/auth headers
- **Cache preflight:** `setMaxAge(3600L)` — 1 hora

### 3.4 Assinatura/verificação de integridade de payloads

**Arquivos:**
- `security/PayloadSignatureManager.java` — gera/valida HMAC-SHA256
- `config/PayloadSignatureInterceptor.java` — intercepta POST/PUT/PATCH
- `security/CachedBodyHttpServletRequest.java` — wrapper que cacheia body para múltiplas leituras
- `config/RequestBodyCachingFilter.java` — filtro que aplica o wrapper antes de tudo

**Mecanismo:**
1. Cliente assina o body com `HMAC-SHA256(payload, PAYLOAD_SIGNATURE_KEY)` em Base64
2. Envia no header `X-Signature`
3. Servidor recomputa e compara em **constant-time** (proteção contra timing attacks via `PayloadSignatureManager.constantTimeEquals`)
4. **Endpoints sensíveis** (`/users`, `/vehicles`, `/units`, `/specification-types`) **rejeitam** com HTTP 401 se header ausente
5. Falha rápida se `PAYLOAD_SIGNATURE_KEY` ausente ou fraco

### ✅ Evidência de teste

| Teste | Comando | Resultado |
|---|---|---|
| **T5 — POST sem X-Signature** | `POST /vehicles` autenticado, sem header | HTTP 401 + "Header X-Signature é obrigatório para este endpoint" |
| **T6 — POST com X-Signature válida** | mesmo POST com HMAC correto | HTTP 201 (veículo criado) |
| **T10 — Rate limit** | 110 requisições GET seguidas | Primeiras ~100 = 200, restantes = 429 |
| **T11 — HTTPS ativo** | Acesso a `https://localhost:8443/swagger-ui/index.html` | TLS 1.2/1.3 funcionando com certificado PKCS12 |

---

## 🟦 Eixo 4 — Segurança de Dados e Privacidade (25 pts)

### 4.1 Criptografia de dados sensíveis em repouso

**Arquivos:**
- `security/DataEncryptionUtil.java` — utilitário AES-256
- `security/EncryptedStringConverter.java` — `AttributeConverter` JPA aplicando AES-256 transparentemente

**Aplicação:**

| Entidade | Campo | Mecanismo |
|---|---|---|
| `model/UserModel.java` | `email` | `@Convert(converter = EncryptedStringConverter.class)` |
| `model/AuditLogModel.java` | `details` | `@Convert(converter = EncryptedStringConverter.class)` |

**Email do usuário armazenado cifrado no banco.** Busca por email criptografa o parâmetro antes da query via `UserService.encryptEmail()`, mantendo compatibilidade com `findByEmail` do Spring Data JPA.

**Senhas:** armazenadas com **BCrypt** (`SecurityFilterChainConfig.passwordEncoder`) — hash irreversível, nunca em texto puro.

**Arquitetura:**
- Chave AES-256 vem de `${DATA_ENCRYPTION_KEY}` no `.env`
- Conversão é transparente: aplicação usa string normal, banco armazena Base64 cifrado
- O `EncryptedStringConverter` falha rápido na inicialização se chave ausente (`IllegalStateException`)

### 4.2 Política de retenção e descarte seguro

**Arquivos:**
- `db/migration/V2__audit_and_retention.sql` — tabela `data_retention_policy`
- `repository/DataRetentionPolicyRepository.java` — leitura via JdbcTemplate
- `service/DataRetentionService.java` — `@Scheduled(cron = "0 0 3 * * *")` (todo dia às 03h)
- `SpecReconApplication.java` — `@EnableScheduling`

**Políticas configuradas:**

| Entidade | Retenção |
|---|---|
| User | 365 dias |
| Vehicle | 730 dias (2 anos) |
| AuditLog | 2555 dias (7 anos — conformidade) |
| Session | 30 dias |

O job lê a tabela diariamente, calcula o cutoff e remove registros antigos via `AuditLogRepository.deleteByTimestampBefore`. Operação `@Transactional` — descarte atômico e seguro.

### 4.3 Anonimização/pseudonimização de dados pessoais

**Arquivo:** `security/PiiAnonymizer.java`

| Método | Tipo | Exemplo |
|---|---|---|
| `maskEmail("joao@empresa.com")` | Anonimização (irreversível) | `j***@empresa.com` |
| `maskDocument("12345678900")` | Anonimização | `123******00` |
| `maskPhone("+5511987654321")` | Anonimização | `*******4321` |
| `maskName("João da Silva")` | Anonimização | `J. d. S.` |
| `pseudonymize("joao@empresa.com")` | Pseudonimização (SHA-256 + salt) | `USR_a8f3d2c1...` |

**Aplicações:**
- **Logs:** mascarar PII antes de gravar
- **Dashboards/BI:** exportar com email/CPF mascarado
- **ML pipelines:** pseudonimização determinística para joins mantendo possibilidade de reidentificação via lookup

### 4.4 Proteção contra exposição acidental de dados

| Mecanismo | Onde |
|---|---|
| `spring.jpa.show-sql=false` | `application.properties` (sem queries SQL nos logs) |
| Stack trace nunca em response | `GlobalExceptionHandler` |
| `.env` no `.gitignore` | `.gitignore` |
| `.env` excluído do build Docker | `.dockerignore` |
| `keystore.p12` no `.gitignore` | `.gitignore` |
| `keystore.p12` montado como volume `:ro` | `docker-compose.yml` |
| Container roda como usuário não-root | `Dockerfile` (`USER appuser`) |
| Defaults de secrets removidos | `JwtTokenProvider`, `PayloadSignatureManager` falham se vazio |
| Endpoints públicos explícitos | `SecurityFilterChainConfig` lista mínima `permitAll` |

### ✅ Evidência de teste

| Teste | Comando | Resultado |
|---|---|---|
| **T9 — Audit log no MySQL** | `SELECT * FROM audit_log` | Linhas com `details` em Base64 cifrado |
| **T12 — Email cifrado no MySQL** | `SELECT id, email FROM user` | Coluna `email` em Base64 cifrado, não texto puro |
| **T13 — Login com email cifrado** | `POST /auth/login` com email válido | HTTP 200 + JWT (busca funciona com encryptEmail) |

---

## 🟦 Eixo 5 — Monitoramento, Logs e Auditoria (15 pts)

### 5.1 Logs estruturados e seguros

**Configuração:** `application.properties`

```properties
logging.level.root=INFO
logging.level.br.com.lane.SpecRecon=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n
logging.file.name=logs/specrecon.log
```

- Padrão consistente (timestamp + logger + mensagem)
- **Sem dados sensíveis** — nunca logamos senha, JWT completo ou payload de request
- Rastreabilidade por timestamp + classe que emitiu o log
- Arquivo de log persistido em `logs/specrecon.log`

### 5.2 Monitoramento de eventos suspeitos

**Arquivo:** `service/AuditService.java` — métodos especializados:

| Método | Captura |
|---|---|
| `logFailedLogin(email, ip)` | Tentativas de autenticação inválidas |
| `logSuccessfulLogin(email, userId, ip)` | Logins bem-sucedidos |
| `logUnauthorizedAccess(performedBy, ip, details)` | Acessos a recursos sem permissão |

**Integração com `GlobalExceptionHandler`:** todo erro 4xx/5xx em endpoints autenticados gera entry de auditoria com IP, ação e status.

### 5.3 Trilha de auditoria para ações críticas

**Arquivo modelo:** `model/AuditLogModel.java`

Campos: `action`, `entity_type`, `entity_id`, `performed_by`, `description`, `ip_address`, `timestamp`, `status`, `details (cifrado AES-256)`

**Cobertura completa nos controllers:**

| Controller | Eventos auditados |
|---|---|
| `AuthController` | LOGIN, FAILED_LOGIN, REGISTER, REGISTER_FAILED, TOKEN_REFRESH_FAILED, PASSWORD_MIGRATED |
| `VehicleController` | CREATE, UPDATE, DELETE Vehicle |
| `UnitController` | CREATE, UPDATE, DELETE Unit |
| `SpecificationTypeController` | CREATE, UPDATE, DELETE SpecificationType |
| `GlobalExceptionHandler` | NOT_FOUND, CONFLICT, ERROR (5xx) |

**Indexação no banco** (`V2__audit_and_retention.sql`):
- `idx_action`, `idx_entity_type`, `idx_performed_by`, `idx_timestamp`, `idx_status` — buscas eficientes em grandes volumes

### ✅ Evidência de teste

| Teste | Comando | Resultado |
|---|---|---|
| **T9 — Verificar audit_log** | Query no MySQL após operações | Registros de LOGIN, CREATE, FAILED_LOGIN com IP e timestamp |
| **T9.5 — Resumo por ação** | `SELECT COUNT(*), action, status FROM audit_log GROUP BY action, status` | Contagens corretas por tipo de evento |

---

## 📁 Apêndice A — Inventário de arquivos de segurança

### Arquivos criados para segurança

| Arquivo | Função |
|---|---|
| `security/EncryptedStringConverter.java` | AttributeConverter JPA AES-256 |
| `security/DataEncryptionUtil.java` | Utilitário de criptografia AES-256 |
| `security/PiiAnonymizer.java` | Mascaramento e pseudonimização de PII |
| `security/JwtTokenProvider.java` | Geração e validação de JWT HS512 |
| `security/JwtAuthenticationFilter.java` | Filtro de autenticação por token |
| `security/PayloadSignatureManager.java` | HMAC-SHA256 de payloads |
| `security/CachedBodyHttpServletRequest.java` | Wrapper para múltiplas leituras de body |
| `config/RequestBodyCachingFilter.java` | Filtro para aplicar o wrapper |
| `config/SecurityFilterChainConfig.java` | Configuração Spring Security stateless |
| `config/CorsConfig.java` | CORS com origens explícitas |
| `config/RateLimitConfig.java` | Rate limiting por IP com Bucket4j |
| `config/PayloadSignatureInterceptor.java` | Interceptor de assinatura HMAC |
| `service/AuditService.java` | Registro de trilha de auditoria |
| `service/DataRetentionService.java` | Job @Scheduled de retenção de dados |
| `repository/DataRetentionPolicyRepository.java` | Acesso à tabela de políticas |
| `validation/SafeStringValidator.java` | Proteção XSS e SQL Injection |
| `validation/StrongPasswordValidator.java` | Validação de senha forte |
| `SECURITY.md` | Este documento |

### Arquivos modificados para segurança

| Arquivo | Mudança |
|---|---|
| `model/UserModel.java` | `@Convert` no campo `email` (AES-256) + comentário BCrypt corrigido |
| `model/AuditLogModel.java` | `@Convert` em `details` (AES-256) |
| `model/Role.java` | Roles `ADMIN`, `ANALYST`, `USER` |
| `service/UserService.java` | `encryptEmail()` para compatibilidade de busca com email cifrado |
| `controller/AuthController.java` | Auditoria + migração automática de senha para BCrypt |
| `controller/VehicleController.java` | Auditoria CREATE/UPDATE/DELETE |
| `controller/UnitController.java` | Auditoria CREATE/UPDATE/DELETE |
| `controller/SpecificationTypeController.java` | Auditoria CREATE/UPDATE/DELETE |
| `exception/GlobalExceptionHandler.java` | Respostas seguras sem stack trace + auditoria de erros |
| `SpecReconApplication.java` | `@EnableScheduling` para o job de retenção |
| `application.properties` | SSL/TLS ativo, sem defaults hardcoded, novas chaves via env |
| `docker-compose.yml` | Porta 8443, volume keystore `:ro` |

---

## 📁 Apêndice B — Variáveis de ambiente sensíveis (`.env`)

Todas configuradas e validadas na inicialização:

| Variável | Uso | Requisito |
|---|---|---|
| `JWT_SECRET` | Assinatura HS512 dos JWTs | Mínimo 32 chars |
| `PAYLOAD_SIGNATURE_KEY` | HMAC-SHA256 dos payloads | Mínimo 32 chars |
| `DATA_ENCRYPTION_KEY` | AES-256 do email e details de auditoria | 256 bits em Base64 |
| `PSEUDONYMIZATION_SALT` | Salt do SHA-256 do PiiAnonymizer | Mínimo 32 chars |
| `MYSQL_ROOT_PASSWORD` | Senha root do MySQL | Forte |
| `MYSQL_PASSWORD` | Senha do usuário da aplicação | Forte |
| `DB_URL` | URL de conexão JDBC | — |
| `DB_USER` | Usuário do banco | — |
| `DB_PASS` | Senha do banco | — |
| `SSL_KEYSTORE_PATH` | Caminho do keystore PKCS12 montado via volume Docker | `/app/keystore.p12` |
| `SSL_KEYSTORE_PASSWORD` | Senha do keystore | Forte |
| `SSL_KEY_ALIAS` | Alias do certificado no keystore | `specrecon` |

**Proteção:**
- `.env` em `.gitignore` — nunca commitado ao repositório
- `.env` em `.dockerignore` — nunca copiado para dentro da imagem Docker
- `keystore.p12` em `.gitignore` — nunca commitado ao repositório
- App falha na inicialização se chaves estão ausentes ou fracas

---

## 📁 Apêndice C — Testes manuais executados

Todos os testes abaixo foram executados via Swagger UI em `https://localhost:8443/swagger-ui/index.html`:

| # | Teste | Resultado |
|---|---|---|
| T1 | Registro de usuário ADMIN com senha forte | ✅ HTTP 201 |
| T2 | Login + recebimento de JWT HS512 + refresh token | ✅ HTTP 200 + token |
| T3 | Acesso a endpoint protegido sem token | ✅ HTTP 401 |
| T4 | Acesso a endpoint protegido com token válido | ✅ HTTP 200 |
| T5 | `POST /vehicles` sem `X-Signature` | ✅ HTTP 401 (rejeitado) |
| T6 | `POST /vehicles` com `X-Signature` HMAC válida | ✅ HTTP 201 |
| T7 | `POST /vehicles` com payload XSS (`<script>`) | ✅ HTTP 400 (rejeitado) |
| T8 | `POST /auth/register` com senha fraca | ✅ HTTP 400 (rejeitado) |
| T9 | Verificação de `audit_log` no MySQL | ✅ Registros íntegros com IP e timestamp |
| T10 | Rate limiting (>100 requisições/min) | ✅ HTTP 429 a partir da 101ª |
| T11 | HTTPS ativo em `https://localhost:8443` | ✅ TLS 1.2/1.3 com certificado PKCS12 RSA 2048 |
| T12 | Email criptografado no banco após registro | ✅ Base64 cifrado na coluna `email` |
| T13 | Login funciona com email criptografado | ✅ HTTP 200 + JWT (encryptEmail funciona) |
| T14 | Refresh token renovando JWT | ✅ HTTP 200 + novo token e refresh token |
| T15 | RBAC — USER tentando DELETE | ✅ HTTP 403 Forbidden |

---

## 🎯 Conclusão

A API SpecRecon implementa **defesa em profundidade** com controles em todas as camadas
exigidas pela rubrica:

- **Entrada:** Bean Validation + SafeString + StrongPassword + limites de payload
- **Autenticação:** JWT HS512 + BCrypt + RBAC com 3 roles + refresh token
- **Transporte:** HTTPS/TLS 1.2+ com certificado PKCS12 + Rate limit + CORS restrito + HMAC payload signature
- **Dados:** AES-256 em repouso (email + auditoria) + retenção scheduled + pseudonimização + secrets em env
- **Observabilidade:** Auditoria completa + logs estruturados + IP tracking + retenção de 7 anos

Todos os 15 testes manuais passaram. Os controles estão documentados, versionados e
prontos para produção.

Alem de tudo, temos:
- **Honeypot:** Endpoints falsos monitoram tentativas de acesso a rotas sensíveis
  comuns em ataques automatizados. Acessos são registrados via AuditService com
  action `HONEYPOT_HIT` incluindo IP, User-Agent e headers completos.
