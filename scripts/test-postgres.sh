#!/usr/bin/env bash
set -euo pipefail

# Отдельный временный кластер: не используем прод-БД, её роли и настройки окружения.
repo_dir=$(cd "$(dirname "$0")/.." && pwd)
pg_bin=${CHALLENGE_TEST_PG_BIN:-/usr/lib/postgresql/18/bin}
pg_tmp=$(mktemp -d /tmp/challenge-lifecycle-pg.XXXXXX)
cleanup() {
    "$pg_bin/pg_ctl" -D "$pg_tmp/data" -m immediate stop >/dev/null 2>&1 || true
    rm -rf "$pg_tmp"
}
trap cleanup EXIT
pg_port=$(python3 - <<'PY'
import socket
with socket.socket() as s:
    s.bind(('127.0.0.1', 0))
    print(s.getsockname()[1])
PY
)
"$pg_bin/initdb" -D "$pg_tmp/data" --auth=trust --username=inactivity_test --no-locale --encoding=UTF8 > "$pg_tmp/init.log"
"$pg_bin/pg_ctl" -D "$pg_tmp/data" -l "$pg_tmp/server.log" -o "-h 127.0.0.1 -p $pg_port -k $pg_tmp -F" -w start
CHALLENGE_TEST_DB_URL="jdbc:postgresql://127.0.0.1:$pg_port/postgres" \
CHALLENGE_TEST_DB_USER=inactivity_test \
    mvn -q -B -f "$repo_dir/pom.xml" -Dtest=ChallengeLifecyclePostgresTest test
