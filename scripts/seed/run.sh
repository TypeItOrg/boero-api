#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."

operation=${1:-load}
case "$operation" in
    load) loader=scripts/seed/enrollment-demo.sql ;;
    repair-ids) loader=scripts/seed/enrollment-demo-repair-ids.sql ;;
    cleanup-legacy) loader=scripts/seed/enrollment-demo-cleanup-legacy.sql ;;
    *) echo "Uso: $0 [load|repair-ids|cleanup-legacy]" >&2; exit 2 ;;
esac
: "${SEED_INSTITUTION:?Indicar SEED_INSTITUTION con el código de la institución destino}"
SEED_YEAR=${SEED_YEAR:-$(date +%Y)}
SEED_DATASET=${SEED_DATASET:-boero-2025}
SEED_TIMEZONE=${SEED_TIMEZONE:-America/Argentina/Cordoba}
SEED_TRANSPORT=${SEED_TRANSPORT:-compose}
# Dataset names are paths within this repository, not arbitrary SQL paths.
if [[ ! "$SEED_DATASET" =~ ^[a-z0-9]+(-[a-z0-9]+)*$ ]]; then
    echo 'SEED_DATASET debe ser un nombre en minúsculas separado por guiones.' >&2; exit 2
fi
if [[ ! "$SEED_YEAR" =~ ^[0-9]{1,4}$ ]]; then
    echo 'SEED_YEAR debe ser un año de 1 a 9998.' >&2; exit 2
fi
if [[ "$operation" != load && "$SEED_DATASET" != boero-2025 ]]; then
    echo 'Las reparaciones históricas corresponden al dataset boero-2025.' >&2; exit 2
fi

# Assemble completely before connecting: a missing file must not commit a partial load.
sql=$(mktemp)
trap 'rm -f "$sql"' EXIT
cat scripts/seed/enrollment-demo-context.sql \
    scripts/seed/enrollment-demo-ids.sql \
    "scripts/seed/datasets/$SEED_DATASET/catalog.sql" \
    "scripts/seed/datasets/$SEED_DATASET/scenario.sql" \
    > "$sql"
if [[ "$operation" == load ]]; then cat scripts/seed/enrollment-demo-validate.sql >> "$sql"; fi
cat "$loader" >> "$sql"
args=(-X -v ON_ERROR_STOP=1
    -v "seed_institution=$SEED_INSTITUTION" -v "seed_year=$SEED_YEAR"
    -v "seed_dataset=$SEED_DATASET" -v "seed_reference_date=${SEED_REFERENCE_DATE:-}"
    -v "seed_timezone=$SEED_TIMEZONE")
case "$SEED_TRANSPORT" in
    compose)
        compose=(docker compose)
        if [[ -n "${SEED_COMPOSE_FILE:-}" ]]; then compose+=(-f "$SEED_COMPOSE_FILE"); fi
        "${compose[@]}" exec -T "${SEED_DB_SERVICE:-postgres}" sh -c \
            'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$@"' sh "${args[@]}" < "$sql"
        ;;
    psql)
        # Standard libpq connection variables: PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD/PGPASSFILE.
        psql "${args[@]}" < "$sql"
        ;;
    *) echo 'SEED_TRANSPORT debe ser compose o psql.' >&2; exit 2 ;;
esac
