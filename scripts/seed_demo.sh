#!/usr/bin/env bash
# BiFriends demo seed (macOS / Linux)
# Usage: ./scripts/seed_demo.sh your@gmail.com
#
# Prerequisites:
#   1. docker-compose up -d db
#   2. Log in once via the app (Google OAuth)

set -euo pipefail

EMAIL="${1:-}"
CONTAINER="${BIFRIENDS_DB_CONTAINER:-bifriends-db}"
DB_USER="${BIFRIENDS_DB_USER:-bifriends}"
DB_NAME="${BIFRIENDS_DB_NAME:-bifriends}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${SCRIPT_DIR}/seed_demo.sql"
TEMP_SQL="$(mktemp)"

usage() {
    echo "Usage: $0 <google-email>"
    exit 1
}

cleanup() { rm -f "$TEMP_SQL"; }
trap cleanup EXIT

[[ -n "$EMAIL" ]] || usage
[[ -f "$SQL_FILE" ]] || { echo "seed_demo.sql not found: $SQL_FILE" >&2; exit 1; }

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
    echo "Docker container '$CONTAINER' is not running." >&2
    echo "Run 'docker-compose up -d db' in bifriends-be first." >&2
    exit 1
fi

ESCAPED_EMAIL="${EMAIL//\'/\'\'}"
sed "s/__DEMO_EMAIL__/${ESCAPED_EMAIL}/g" "$SQL_FILE" > "$TEMP_SQL"

echo ">> Seeding demo data — email=$EMAIL, container=$CONTAINER"

docker cp "$TEMP_SQL" "${CONTAINER}:/tmp/seed_demo.sql"
docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f /tmp/seed_demo.sql

echo ""
echo "Done! Reopen the app to verify."
echo "  Parent mode PIN: 1234"
echo "  available_pool: 30"
echo "  25 learning attempts, 3 chat sessions"
