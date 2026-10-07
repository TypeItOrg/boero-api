#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."
: "${SEED_INSTITUTION:?Indicar código de la institución local/de prueba}"
SEED_YEAR=${SEED_YEAR:-$(date +%Y)}
[[ "$SEED_YEAR" =~ ^[0-9]{4}$ ]] || { echo 'SEED_YEAR debe tener cuatro dígitos.' >&2; exit 2; }
# Transport is intentionally local Compose only; do not run against staging/production.
sql=$(mktemp)
trap 'rm -f "$sql"' EXIT
cat scripts/seed/enrollment-demo-context.sql scripts/seed/enrollment-demo-ids.sql > "$sql"
sed '/^\\ir /d' scripts/seed/enrollment-ui-polish.sql >> "$sql"
docker compose exec -T postgres sh -c 'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$@"' sh \
  -X -v ON_ERROR_STOP=1 -v "seed_institution=$SEED_INSTITUTION" -v "seed_year=$SEED_YEAR" \
  -v seed_dataset=enrollment-ui-polish -v seed_reference_date= -v seed_timezone=America/Argentina/Cordoba \
  < "$sql"
