#!/bin/sh
set -eu

docker-entrypoint.sh postgres &
postgres_pid=$!

cleanup() {
  kill "$postgres_pid" 2>/dev/null || true
  wait "$postgres_pid" 2>/dev/null || true
}

trap cleanup INT TERM

until pg_isready -U "$POSTGRES_USER" -d postgres >/dev/null 2>&1; do
  sleep 1
done

db_exists="$(psql -U "$POSTGRES_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'")"

if [ "$db_exists" != "1" ]; then
  psql -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE \"$DB_NAME\""
fi

wait "$postgres_pid"
