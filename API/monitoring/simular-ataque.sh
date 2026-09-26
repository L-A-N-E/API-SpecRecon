#!/usr/bin/env bash
# =====================================================================
# SpecRecon - Simulação de ataque para validar alertas (Sprint 3 - Etapa 3)
#
# Uso (dentro de API/, com API + monitoramento rodando e o ADMIN já criado):
#   bash monitoring/simular-ataque.sh
#
# Gera, contra a própria API local, os eventos que disparam os alertas:
#   HoneypotHit, AccessDeniedSpike, RoleChanged, PrivilegeEscalationAttempt,
#   BruteForceLogin e RateLimitAbuse.
# Depois de ~30s, veja: Grafana (localhost:3000) e Prometheus > Alerts (localhost:9091).
#
# Obs.: /auth tem limite de 10 req/min por IP. O script usa esse "orçamento"
# de propósito, na ordem certa. Se rodar de novo, espere 1 minuto.
# =====================================================================
set -uo pipefail

BASE="${BASE:-https://localhost:8443}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@ford.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-@Securepassword123}"
PASS="@Securepassword123"
KEY="$(grep -E '^PAYLOAD_SIGNATURE_KEY=' .env | cut -d= -f2- | tr -d '\r')"
RUN="$(date +%s)"
C="curl -sk"

sign() { echo -n "$1" | openssl dgst -sha256 -hmac "$KEY" -binary | base64; }
code() { $C -o /dev/null -w "%{http_code}" "$@"; }

echo "== 1. Reconhecimento: honeypot =="
for p in /.env /admin /actuator/env /backup; do echo "  GET $p -> $(code "$BASE$p")"; done

echo "== 2. Usuário comum tentando acessar /users (403 em série) =="
USER_EMAIL="user-sim-$RUN@ford.com"
REG=$($C -X POST "$BASE/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_EMAIL\",\"password\":\"$PASS\",\"role\":\"USER\"}")
USER_ID=$(echo "$REG" | grep -o '"id":[0-9]*' | cut -d: -f2)
USER_TOKEN=$($C -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_EMAIL\",\"password\":\"$PASS\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
for i in $(seq 1 12); do code "$BASE/users" -H "Authorization: Bearer $USER_TOKEN" >/dev/null; done
echo "  12x GET /users com perfil USER -> 403"

echo "== 3. Troca de perfil feita por ADMIN (USER -> ANALYST) =="
ADMIN_TOKEN=$($C -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
BODY="{\"email\":\"$USER_EMAIL\",\"password\":\"$PASS\",\"role\":\"ANALYST\"}"
echo "  PUT /users/$USER_ID -> $(code -X PUT "$BASE/users/$USER_ID" -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "X-Signature: $(sign "$BODY")" -d "$BODY")"

echo "== 4. Escalada de privilégio: auto-registro como ADMIN =="
echo "  POST /auth/register role=ADMIN -> $(code -X POST "$BASE/auth/register" -H "Content-Type: application/json" \
  -d "{\"email\":\"hacker-$RUN@ford.com\",\"password\":\"$PASS\",\"role\":\"ADMIN\"}")"

echo "== 5. Brute force de login (até o rate limit bloquear) =="
for i in $(seq 1 9); do
  echo "  tentativa $i -> $(code -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"senhaErrada$i\"}")"
done

echo
echo "Pronto. Aguarde ~30s e confira:"
echo "  Grafana:    http://localhost:3000  (dashboard 'SpecRecon - Segurança e Observabilidade')"
echo "  Prometheus: http://localhost:9091/alerts"
