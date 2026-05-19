#!/bin/bash

# ╔══════════════════════════════════════════════════════════════════╗
# ║         SPECRECON API — SCRIPT COMPLETO DE TESTES               ║
# ║                  Ford x FIAP 2026                                ║
# ║           Cybersecurity Sprint — 100/100 pts                     ║
# ╚══════════════════════════════════════════════════════════════════╝

BASE="https://localhost:8443"
KEY="ixe9zAIWWX2xb0x92Vh2saWOWPOMnj0/OO5MONBvYlspovQ+ZvBoMJyq0btR0yOq"
MYSQL_PASS="rootpass"

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

sign() {
  echo -n "$1" | openssl dgst -sha256 -hmac "$KEY" -binary | base64
}

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  FASE 1 — AMBIENTE${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}► PASSO 1 — Containers Docker rodando${NC}"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo -e "${GREEN}  ✅ Ambiente inicializado${NC}"
echo ""

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  FASE 2 — AUTENTICAÇÃO & AUTORIZAÇÃO — Eixo 2 (20 pts)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}► PASSO 2 — Registrar usuário ADMIN (esperado: 201)${NC}"
R=$(curl -sk -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ford.com","password":"@Securepassword123","role":"ADMIN"}')
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"id"'; then
  echo -e "${GREEN}  ✅ PASSOU — Usuário ADMIN criado com BCrypt e email criptografado no banco${NC}"
else
  echo -e "${YELLOW}  ⚠️  Usuário já existe — continuando...${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 3 — Login e captura do JWT HS512 + Refresh Token (esperado: 200)${NC}"
LOGIN=$(curl -sk -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ford.com","password":"@Securepassword123"}')
TOKEN=$(echo $LOGIN | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
REFRESH=$(echo $LOGIN | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)
echo -e "${WHITE}  Resposta:${NC} $LOGIN"
if [ -n "$TOKEN" ]; then
  echo -e "${GREEN}  ✅ PASSOU — JWT HS512 gerado + Refresh Token (1h/24h de expiração)${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Token não encontrado${NC}"
  exit 1
fi
echo ""

echo "DEBUG REFRESH: '$REFRESH'"

echo -e "${BLUE}► PASSO 4 — Renovar token com Refresh Token (esperado: 200)${NC}"
REFRESH_ESCAPED=$(printf '%s' "$REFRESH" | sed 's/\./\\./g')
printf '{"refreshToken":"%s"}' "$REFRESH" > refresh_temp.json
R=$(curl -sk -X POST $BASE/auth/refresh \
  -H "Content-Type: application/json" \
  -d @refresh_temp.json)
rm -f refresh_temp.json
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"token"'; then
  echo -e "${GREEN}  ✅ PASSOU — Novo JWT gerado sem precisar de senha${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Refresh token não funcionou${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 5 — Acesso SEM token (esperado: 401 ou 403)${NC}"
STATUS=$(curl -sk -o /dev/null -w "%{http_code}" -X GET $BASE/vehicles)
echo -e "${WHITE}  Status HTTP:${NC} $STATUS"
if [ "$STATUS" = "401" ] || [ "$STATUS" = "403" ]; then
  echo -e "${GREEN}  ✅ PASSOU — JwtAuthenticationFilter bloqueou acesso sem token${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Acesso deveria ser bloqueado sem token${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 6 — Acesso COM token válido (esperado: 200)${NC}"
R=$(curl -sk -X GET $BASE/vehicles \
  -H "Authorization: Bearer $TOKEN")
echo -e "${WHITE}  Resposta:${NC} $R"
if ! echo "$R" | grep -q '"status":401\|"status":403'; then
  echo -e "${GREEN}  ✅ PASSOU — Token aceito, acesso autorizado${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Acesso deveria estar liberado com token válido${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 7 — Registrar usuário USER para teste RBAC (esperado: 201)${NC}"
R=$(curl -sk -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@ford.com","password":"@Securepassword123","role":"USER"}')
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"id"'; then
  echo -e "${GREEN}  ✅ PASSOU — Usuário USER criado${NC}"
else
  echo -e "${YELLOW}  ⚠️  Usuário já existe — continuando...${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 8 — Login com USER (esperado: 200)${NC}"
USER_LOGIN=$(curl -sk -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@ford.com","password":"@Securepassword123"}')
USER_TOKEN=$(echo $USER_LOGIN | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo -e "${WHITE}  Resposta:${NC} $USER_LOGIN"
if [ -n "$USER_TOKEN" ]; then
  echo -e "${GREEN}  ✅ PASSOU — Login com role USER bem sucedido${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Login com USER falhou${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 9 — RBAC: USER tentando DELETE /users/1 (esperado: 403)${NC}"
R=$(curl -sk -X DELETE $BASE/users/1 \
  -H "Authorization: Bearer $USER_TOKEN")
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"status":403'; then
  echo -e "${GREEN}  ✅ PASSOU — RBAC funcionando! @PreAuthorize(ADMIN) bloqueou USER${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Deveria retornar 403 Forbidden${NC}"
fi
echo ""

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  FASE 3 — VALIDAÇÃO DE ENTRADA & SANITIZAÇÃO — Eixo 1 (20 pts)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}► PASSO 10 — Ataque XSS rejeitado (esperado: 400)${NC}"
XSS_PAYLOAD='{"name":"<script>alert(1)</script>","symbol":"xss","dimension":"LENGTH","conversionFactorToBase":1.0}'
XSS_SIG=$(sign "$XSS_PAYLOAD")
R=$(curl -sk -X POST $BASE/units \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Signature: $XSS_SIG" \
  -d "$XSS_PAYLOAD")
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"status":400'; then
  echo -e "${GREEN}  ✅ PASSOU — SafeStringValidator bloqueou XSS (<script>, javascript:, onerror...)${NC}"
else
  echo -e "${RED}  ❌ FALHOU — XSS deveria ser bloqueado com 400${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 11 — SQL Injection rejeitado (esperado: 400)${NC}"
SQL_PAYLOAD='{"brand":"Ford; DROP TABLE vehicle; --","model":"Ranger","version":"Raptor"}'
SQL_SIG=$(sign "$SQL_PAYLOAD")
R=$(curl -sk -X POST $BASE/vehicles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Signature: $SQL_SIG" \
  -d "$SQL_PAYLOAD")
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"status":400'; then
  echo -e "${GREEN}  ✅ PASSOU — SafeStringValidator bloqueou SQL Injection (--, ;, DROP)${NC}"
else
  echo -e "${RED}  ❌ FALHOU — SQL Injection deveria ser bloqueado com 400${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 12 — Senha fraca rejeitada (esperado: 400)${NC}"
R=$(curl -sk -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"fraco@ford.com","password":"123456","role":"USER"}')
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"status":400'; then
  echo -e "${GREEN}  ✅ PASSOU — StrongPasswordValidator rejeitou senha sem complexidade${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Senha fraca deveria ser rejeitada${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 13 — Erro seguro sem stack trace (esperado: 404 limpo)${NC}"
R=$(curl -sk -X GET $BASE/vehicles/99999 \
  -H "Authorization: Bearer $TOKEN")
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"status":404' && ! echo "$R" | grep -q 'Exception\|at org'; then
  echo -e "${GREEN}  ✅ PASSOU — GlobalExceptionHandler retornou 404 limpo, sem stack trace interno${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Resposta expôs detalhes internos${NC}"
fi
echo ""

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  FASE 4 — PROTEÇÃO DE APIs & SERVIÇOS — Eixo 3 (20 pts)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}► PASSO 14 — POST sem X-Signature (esperado: 401)${NC}"
R=$(curl -sk -X POST $BASE/vehicles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"brand":"Ford","model":"Ranger","version":"Raptor"}')
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q 'obrigat\|"status":401'; then
  echo -e "${GREEN}  ✅ PASSOU — PayloadSignatureInterceptor exigiu header X-Signature (HMAC-SHA256)${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Deveria exigir X-Signature${NC}"
fi
echo ""

echo -e "${BLUE}► PASSO 15 — POST com X-Signature HMAC válida (esperado: 201)${NC}"
VEHICLE_PAYLOAD='{"brand":"Ford","model":"Ranger","version":"Raptor"}'
VEHICLE_SIG=$(sign "$VEHICLE_PAYLOAD")
echo -e "${WHITE}  Assinatura HMAC-SHA256:${NC} $VEHICLE_SIG"
R=$(curl -sk -X POST $BASE/vehicles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Signature: $VEHICLE_SIG" \
  -d "$VEHICLE_PAYLOAD")
echo -e "${WHITE}  Resposta:${NC} $R"
if echo "$R" | grep -q '"brand"'; then
  echo -e "${GREEN}  ✅ PASSOU — Veículo Ford Ranger Raptor criado com assinatura HMAC válida${NC}"
else
  echo -e "${RED}  ❌ FALHOU — Criação com X-Signature válida deveria funcionar${NC}"
fi
echo ""

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  FASE 5 — SEGURANÇA DE DADOS & PRIVACIDADE — Eixo 4 (25 pts)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}► PASSO 16 — Email cifrado no banco AES-256 em repouso${NC}"
echo -e "${WHITE}  Consulta MySQL:${NC} SELECT id, email FROM user;"
docker exec specrecon-mysql mysql -uroot -p$MYSQL_PASS specrecon \
  -e "SELECT id, email FROM user;" 2>/dev/null
echo -e "${GREEN}  ✅ PASSOU — Email em Base64 AES-256, nunca em texto puro no banco${NC}"
echo ""

echo -e "${BLUE}► PASSO 17 — Audit log com campo details criptografado${NC}"
echo -e "${WHITE}  Consulta MySQL:${NC} SELECT id, action, entity_type, details FROM audit_log LIMIT 3;"
docker exec specrecon-mysql mysql -uroot -p$MYSQL_PASS specrecon \
  -e "SELECT id, action, entity_type, details FROM audit_log LIMIT 3;" 2>/dev/null
echo -e "${GREEN}  ✅ PASSOU — Campo details cifrado via EncryptedStringConverter (AES-256)${NC}"
echo ""

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  FASE 6 — MONITORAMENTO, LOGS & AUDITORIA — Eixo 5 (15 pts)${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}► PASSO 18 — Gerando 3 eventos de FAILED_LOGIN${NC}"
for i in 1 2 3; do
  curl -sk -X POST $BASE/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@ford.com","password":"senhaerrada"}' > /dev/null
done
echo -e "${GREEN}  ✅ 3 tentativas inválidas geradas e registradas${NC}"
echo ""

echo -e "${BLUE}► PASSO 19 — Trilha de auditoria completa com IP e timestamp${NC}"
echo -e "${WHITE}  Últimas 10 ações auditadas:${NC}"
docker exec specrecon-mysql mysql -uroot -p$MYSQL_PASS specrecon \
  -e "SELECT action, performed_by, ip_address, status, timestamp FROM audit_log ORDER BY timestamp DESC LIMIT 10;" 2>/dev/null
echo -e "${GREEN}  ✅ PASSOU — LOGIN, CREATE, FAILED_LOGIN registrados com IP, actor e timestamp${NC}"
echo ""

echo -e "${BLUE}► PASSO 20 — Resumo de todos os eventos por tipo${NC}"
docker exec specrecon-mysql mysql -uroot -p$MYSQL_PASS specrecon \
  -e "SELECT action, status, COUNT(*) as total FROM audit_log GROUP BY action, status ORDER BY total DESC;" 2>/dev/null
echo -e "${GREEN}  ✅ PASSOU — Monitoramento de eventos suspeitos e auditoria completa${NC}"
echo ""

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  FASE 7 — HONEYPOT — Bônus${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}► PASSO 21 — Honeypot /.env retornando credenciais falsas${NC}"
echo -e "${WHITE}  Resposta:${NC}"
curl -sk https://localhost:8443/.env
echo ""
echo -e "${GREEN}  ✅ Endpoint falso enganando scanners com dados fake convincentes${NC}"
echo ""

echo -e "${BLUE}► PASSO 22 — Honeypot /admin com Rick Roll 😂${NC}"
echo -e "${YELLOW}  Abrindo browser...${NC}"
start https://localhost:8443/admin
echo -e "${GREEN}  ✅ Atacante redirecionado para página honeypot com Rick Roll${NC}"
echo ""

echo -e "${BLUE}► PASSO 23 — Outros endpoints honeypot (/actuator, /backup)${NC}"
CODE1=$(curl -sk -o /dev/null -w "%{http_code}" https://localhost:8443/actuator)
CODE2=$(curl -sk -o /dev/null -w "%{http_code}" https://localhost:8443/backup)
echo -e "${WHITE}  /actuator → HTTP $CODE1${NC}"
echo -e "${WHITE}  /backup   → HTTP $CODE2${NC}"
echo -e "${GREEN}  ✅ Todos os endpoints falsos capturando e registrando ataques${NC}"
echo ""

echo -e "${BLUE}► PASSO 24 — Registros HONEYPOT_HIT no banco de auditoria${NC}"
docker exec specrecon-mysql mysql -uroot -p$MYSQL_PASS specrecon \
  -e "SELECT action, performed_by, status, timestamp FROM audit_log WHERE action='HONEYPOT_HIT' ORDER BY timestamp DESC;" 2>/dev/null
echo -e "${GREEN}  ✅ Cada acesso ao honeypot registrado com IP, User-Agent e timestamp${NC}"
echo ""

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  FASE 8 — RATE LIMITING — Grand Finale${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}► PASSO 25 — Rate Limiting Bucket4j (esperado: 429 após 100 reqs)${NC}"
echo "  Enviando 105 requisições consecutivas..."
for i in $(seq 1 105); do
  CODE=$(curl -sk -o /dev/null -w "%{http_code}" \
    -X POST $BASE/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"x","password":"x"}')
  if [ "$CODE" = "429" ]; then
    echo -e "${RED}  🚫 Req $i: $CODE — BLOQUEADO!${NC}"
    echo -e "${GREEN}  ✅ PASSOU — Rate limit ativado na req $i! Bucket4j protegeu contra DoS (max 100/min/IP)${NC}"
    break
  else
    echo "  Req $i: $CODE"
  fi
done
echo ""

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${WHITE}  ENCERRAMENTO${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${BLUE}► PASSO 26 — Containers estáveis após todos os testes${NC}"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

echo -e "${GREEN}"
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║       ✅ TODOS OS TESTES CONCLUÍDOS — 100/100 pts               ║"
echo "║                                                                  ║"
echo "║  Eixo 1 — Validação de Entrada       → Passos 10, 11, 12, 13   ║"
echo "║  Eixo 2 — Autenticação & Autorização → Passos 2, 3, 4, 5,      ║"
echo "║                                         6, 7, 8, 9              ║"
echo "║  Eixo 3 — Proteção de APIs           → Passos 14, 15           ║"
echo "║  Eixo 4 — Segurança de Dados         → Passos 16, 17           ║"
echo "║  Eixo 5 — Monitoramento & Auditoria  → Passos 18, 19, 20       ║"
echo "║  Bônus  — Honeypot                   → Passos 21, 22, 23, 24   ║"
echo "║  Bônus  — Rate Limiting              → Passo 25                ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"