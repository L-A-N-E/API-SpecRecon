#!/usr/bin/env bash
# =====================================================================
# SpecRecon - Restore do MySQL (Sprint 3 - Etapa 3: Recuperação / PICERL)
#
# Uso (dentro da pasta API/):
#   bash scripts/restore.sh                      -> restaura o backup MAIS RECENTE
#   bash scripts/restore.sh backups/arquivo.sql.gz
#
# Segurança:
#   - confere o SHA-256 antes de restaurar: backup adulterado/corrompido é RECUSADO
#   - pede confirmação explícita (restore sobrescreve o banco atual)
# =====================================================================
set -euo pipefail

CONTAINER="${MYSQL_CONTAINER:-specrecon-mysql}"
BACKUP_DIR="${BACKUP_DIR:-backups}"
ENV_FILE="${ENV_FILE:-.env}"

read_env() {
  grep -E "^$1=" "$ENV_FILE" | head -n1 | cut -d= -f2- | tr -d '\r'
}

[ -f "$ENV_FILE" ] || { echo "ERRO: $ENV_FILE não encontrado (rode dentro da pasta API/)"; exit 1; }
DB_ROOT_PASS="$(read_env MYSQL_ROOT_PASSWORD)"

FILE="${1:-$(ls -1t "$BACKUP_DIR"/specrecon_*.sql.gz 2>/dev/null | head -n1)}"
[ -n "$FILE" ] && [ -f "$FILE" ] || { echo "ERRO: nenhum backup encontrado"; exit 1; }

echo "[restore] Arquivo: $FILE"

# 1. Integridade
if [ ! -f "$FILE.sha256" ]; then
  echo "ERRO: $FILE.sha256 ausente - integridade não pode ser comprovada"; exit 1
fi
if ! sha256sum -c "$FILE.sha256" >/dev/null 2>&1; then
  echo "ERRO: SHA-256 NÃO confere - backup corrompido ou adulterado. Restore recusado."; exit 1
fi
echo "[restore] Integridade OK (SHA-256 confere)"

# 2. Confirmação
read -r -p "[restore] Isso vai SOBRESCREVER o banco atual. Digite 'RESTAURAR' para continuar: " CONFIRM
[ "$CONFIRM" = "RESTAURAR" ] || { echo "[restore] Cancelado."; exit 0; }

# 3. Restore
gzip -dc "$FILE" | docker exec -i -e MYSQL_PWD="$DB_ROOT_PASS" "$CONTAINER" mysql -uroot
echo "[restore] Banco restaurado a partir de $FILE"
echo "[restore] Recomendado: reiniciar a API (docker compose restart api) e validar login/listagens."
