#!/usr/bin/env bash
# =====================================================================
# SpecRecon - Backup do MySQL (Sprint 3 - Etapa 3: Recuperação / PICERL)
#
# Uso (dentro da pasta API/, com o docker compose rodando):
#   bash scripts/backup.sh
#
# O que faz:
#   1. mysqldump consistente (--single-transaction) de dentro do container
#   2. compacta com gzip e nomeia com data/hora
#   3. gera hash SHA-256 (prova de integridade usada pelo restore.sh)
#   4. mantém somente os N backups mais recentes (retenção)
#
# Segurança:
#   - senha lida do .env e passada via variável MYSQL_PWD (não aparece no "ps")
#   - pasta backups/ está no .gitignore (dump NUNCA vai para o GitHub)
#   - e-mails e detalhes de auditoria já estão cifrados (AES-GCM) dentro do dump,
#     e senhas estão em BCrypt
# =====================================================================
set -euo pipefail

CONTAINER="${MYSQL_CONTAINER:-specrecon-mysql}"
BACKUP_DIR="${BACKUP_DIR:-backups}"
RETENTION="${BACKUP_RETENTION:-7}"
ENV_FILE="${ENV_FILE:-.env}"

# Lê uma chave do .env (ignora CRLF do Windows)
read_env() {
  grep -E "^$1=" "$ENV_FILE" | head -n1 | cut -d= -f2- | tr -d '\r'
}

[ -f "$ENV_FILE" ] || { echo "ERRO: $ENV_FILE não encontrado (rode dentro da pasta API/)"; exit 1; }
DB_NAME="$(read_env MYSQL_DATABASE)"
DB_ROOT_PASS="$(read_env MYSQL_ROOT_PASSWORD)"
[ -n "$DB_NAME" ] && [ -n "$DB_ROOT_PASS" ] || { echo "ERRO: MYSQL_DATABASE/MYSQL_ROOT_PASSWORD ausentes no .env"; exit 1; }

docker ps --format '{{.Names}}' | grep -qx "$CONTAINER" \
  || { echo "ERRO: container $CONTAINER não está rodando (docker compose up -d)"; exit 1; }

mkdir -p "$BACKUP_DIR"
STAMP="$(date +%Y%m%d_%H%M%S)"
FILE="$BACKUP_DIR/specrecon_${STAMP}.sql.gz"

echo "[backup] Gerando dump do banco '$DB_NAME'..."
docker exec -e MYSQL_PWD="$DB_ROOT_PASS" "$CONTAINER" \
  mysqldump -uroot --single-transaction --routines --triggers --databases "$DB_NAME" \
  | gzip > "$FILE"

# Dump vazio = falha silenciosa: aborta
if [ ! -s "$FILE" ] || [ "$(gzip -dc "$FILE" | head -c 1 | wc -c)" -eq 0 ]; then
  rm -f "$FILE"
  echo "ERRO: dump vazio, backup abortado"; exit 1
fi

sha256sum "$FILE" > "$FILE.sha256"
echo "[backup] OK: $FILE ($(du -h "$FILE" | cut -f1))"
echo "[backup] SHA-256: $(cut -d' ' -f1 "$FILE.sha256")"

# Retenção: remove os mais antigos além de $RETENTION
ls -1t "$BACKUP_DIR"/specrecon_*.sql.gz 2>/dev/null | tail -n +"$((RETENTION + 1))" | while read -r old; do
  rm -f "$old" "$old.sha256"
  echo "[backup] Retenção: removido $old"
done
