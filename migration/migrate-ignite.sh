#!/usr/bin/env bash
# =============================================================================
# Скрипт миграции Apache Ignite 3.0.0 → 3.1.0 для ChallengeDiscordBot
#
# Что делает:
#   1. Экспортирует данные из работающего Ignite 3.0.0
#   2. Останавливает бота
#   3. Делает снапшот work-директории Ignite
#   4. Останавливает Ignite 3.0.0, устанавливает 3.1.0
#   5. Запускает Ignite 3.1.0 и импортирует данные
#   6. Запускает бота, верифицирует данные
#
# Использование:
#   ./migrate-ignite.sh                  — полный цикл миграции
#   ./migrate-ignite.sh export-only      — только экспорт данных (без остановки)
#   ./migrate-ignite.sh import-only      — только импорт (если Ignite 3.1.0 уже запущен)
#   ./migrate-ignite.sh verify-only      — только сверка backup vs live
#   ./migrate-ignite.sh rollback         — откат на Ignite 3.0.0 из work-снапшота
#
# Требования:
#   - Java 17+
#   - challenge-bot-1.0.0.jar в $BOT_DIR
#   - sudo-права для управления сервисами (или запуск от root)
# =============================================================================
set -euo pipefail

# ── Конфигурация ─────────────────────────────────────────────────────────────
IGNITE_DIR="/opt/ignite3"
IGNITE_WORK_DIR="$IGNITE_DIR/work"
BOT_DIR="/opt/challengeBot"
BOT_JAR="$BOT_DIR/challenge-bot-1.0.0.jar"
BOT_SH="$BOT_DIR/challengeBot.sh"
IGNITE_ADDRESS="127.0.0.1:10300"
IGNITE_REST="http://localhost:3344"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/ignite-migration-$TIMESTAMP"
WORK_SNAPSHOT="$IGNITE_DIR/work-snapshot-$TIMESTAMP"

IGNITE_310_URL="https://archive.apache.org/dist/ignite/3.1.0/apache-ignite-3.1.0.zip"
IGNITE_310_DIR="/opt/ignite3-new"
# ─────────────────────────────────────────────────────────────────────────────

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
ok()   { echo "[$(date '+%H:%M:%S')] ✓ $*"; }
warn() { echo "[$(date '+%H:%M:%S')] ⚠ $*"; }
die()  { echo "[$(date '+%H:%M:%S')] ✗ $*" >&2; exit 1; }

# ── Шаг 0: Проверка зависимостей ─────────────────────────────────────────────
check_deps() {
    log "Проверка зависимостей..."
    command -v java  >/dev/null || die "java не найден"
    command -v curl  >/dev/null || die "curl не найден"
    command -v unzip >/dev/null || die "unzip не найден"
    [[ -f "$BOT_JAR" ]] || die "JAR бота не найден: $BOT_JAR"
    ok "Зависимости в порядке"
}

# ── Шаг 1: Экспорт данных ─────────────────────────────────────────────────────
export_data() {
    log "Экспорт данных из Ignite $IGNITE_ADDRESS в $BACKUP_DIR ..."
    mkdir -p "$BACKUP_DIR"

    java -cp "$BOT_JAR" com.discord.challengebot.util.DataExport \
        export "$BACKUP_DIR" "$IGNITE_ADDRESS" \
        || die "Экспорт завершился с ошибкой"

    log "Экспортированные файлы:"
    ls -lh "$BACKUP_DIR"/
    ok "Экспорт завершён: $BACKUP_DIR"
}

# ── Шаг 2: Остановка бота ─────────────────────────────────────────────────────
stop_bot() {
    log "Остановка ChallengeBot..."
    if "$BOT_SH" status 2>/dev/null | grep -q "running"; then
        "$BOT_SH" stop || warn "Ошибка при остановке бота (продолжаем)"
        sleep 3
    fi
    ok "Бот остановлен"
}

# ── Шаг 3: Снапшот work-директории Ignite ─────────────────────────────────────
snapshot_work() {
    log "Снапшот work-директории: $IGNITE_WORK_DIR → $WORK_SNAPSHOT ..."
    cp -r "$IGNITE_WORK_DIR" "$WORK_SNAPSHOT"
    ok "Снапшот создан: $WORK_SNAPSHOT"
}

# ── Шаг 4: Остановка Ignite 3.0.0 ─────────────────────────────────────────────
stop_ignite_300() {
    log "Остановка Ignite 3.0.0..."
    PID=$(pgrep -f "ignite-runner-3.0.0.jar" || true)
    if [[ -n "$PID" ]]; then
        kill "$PID"
        for i in $(seq 1 12); do
            sleep 5
            pgrep -f "ignite-runner-3.0.0.jar" >/dev/null 2>&1 || break
            log "Ожидаем остановки Ignite... ($((i*5))s)"
        done
        pgrep -f "ignite-runner-3.0.0.jar" >/dev/null 2>&1 && kill -9 "$PID" || true
    fi
    ok "Ignite 3.0.0 остановлен"
}

# ── Шаг 5: Установка Ignite 3.1.0 ─────────────────────────────────────────────
install_ignite_310() {
    log "Загрузка Apache Ignite 3.1.0..."
    TMP_ZIP="/tmp/ignite-3.1.0.zip"

    if [[ ! -f "$TMP_ZIP" ]]; then
        curl -L "$IGNITE_310_URL" -o "$TMP_ZIP" || die "Не удалось загрузить Ignite 3.1.0"
    fi

    log "Установка Ignite 3.1.0 в $IGNITE_310_DIR ..."
    rm -rf "$IGNITE_310_DIR"
    mkdir -p "$IGNITE_310_DIR"
    unzip -q "$TMP_ZIP" -d "$IGNITE_310_DIR"
    # Перемещаем содержимое из вложенной папки наверх
    EXTRACTED=$(find "$IGNITE_310_DIR" -maxdepth 1 -type d | tail -1)
    if [[ "$EXTRACTED" != "$IGNITE_310_DIR" ]]; then
        mv "$EXTRACTED"/* "$IGNITE_310_DIR"/ 2>/dev/null || true
    fi

    ok "Ignite 3.1.0 установлен в $IGNITE_310_DIR"
}

# ── Шаг 6: Запуск Ignite 3.1.0 ────────────────────────────────────────────────
start_ignite_310() {
    log "Запуск Ignite 3.1.0..."
    IGNITE_310_BIN="$IGNITE_310_DIR/bin/ignite3db"
    [[ -f "$IGNITE_310_BIN" ]] || IGNITE_310_BIN=$(find "$IGNITE_310_DIR" -name "ignite3db" -type f | head -1)
    [[ -f "$IGNITE_310_BIN" ]] || die "ignite3db не найден в $IGNITE_310_DIR"

    nohup "$IGNITE_310_BIN" \
        --config-path "$IGNITE_DIR/etc/ignite-config.conf" \
        --work-dir "$IGNITE_WORK_DIR" \
        --node-name defaultNode \
        > "$IGNITE_DIR/log/ignite3.1.log" 2>&1 &

    log "Ожидаем запуска Ignite 3.1.0 (до 60 секунд)..."
    for i in $(seq 1 12); do
        sleep 5
        STATUS=$(curl -s "$IGNITE_REST/management/v1/node/state" 2>/dev/null | grep -o '"state":"STARTED"' || true)
        if [[ -n "$STATUS" ]]; then
            VERSION=$(curl -s "$IGNITE_REST/management/v1/node/version" 2>/dev/null)
            ok "Ignite запущен: $VERSION"
            return 0
        fi
        log "Ожидаем... ($((i*5))s)"
    done
    die "Ignite 3.1.0 не запустился за 60 секунд. Лог: $IGNITE_DIR/log/ignite3.1.log"
}

# ── Шаг 7: Инициализация кластера (если новая work-директория) ─────────────────
init_cluster_if_needed() {
    CLUSTER_STATE=$(curl -s "$IGNITE_REST/management/v1/cluster/state" 2>/dev/null || true)
    if echo "$CLUSTER_STATE" | grep -q '"cmgNodes"'; then
        ok "Кластер уже инициализирован"
        return 0
    fi
    log "Инициализация кластера Ignite 3.1.0..."
    curl -s -X POST "$IGNITE_REST/management/v1/cluster/init" \
        -H "Content-Type: application/json" \
        -d '{"metaStorageNodes":["defaultNode"],"clusterName":"bchgrp-cluster"}' \
        || die "Ошибка инициализации кластера"
    sleep 10
    ok "Кластер инициализирован"
}

# ── Шаг 8: Импорт данных ───────────────────────────────────────────────────────
import_data() {
    log "Импорт данных из $BACKUP_DIR в Ignite $IGNITE_ADDRESS ..."
    java -cp "$BOT_JAR" com.discord.challengebot.util.DataExport \
        import "$BACKUP_DIR" "$IGNITE_ADDRESS" \
        || die "Импорт завершился с ошибкой"
    ok "Импорт завершён"
}

# ── Шаг 9: Запуск бота ────────────────────────────────────────────────────────
start_bot() {
    log "Запуск ChallengeBot..."
    "$BOT_SH" start || die "Не удалось запустить бота"
    sleep 5
    ok "Бот запущен"
}

# ── Шаг 10: Верификация данных ──────────────────────────────────────────────────
verify_data() {
    log "Верификация данных (backup vs live)..."
    java -cp "$BOT_JAR" com.discord.challengebot.util.DataExport \
        verify "$BACKUP_DIR" "$IGNITE_ADDRESS" \
        || die "Верификация провалена — количество строк не совпадает!"
    ok "Верификация пройдена — все данные на месте"
}

# ── ROLLBACK ──────────────────────────────────────────────────────────────────
rollback() {
    warn "=== ОТКАТ НА Ignite 3.0.0 ==="
    [[ -d "$WORK_SNAPSHOT" ]] || die "Снапшот не найден: $WORK_SNAPSHOT"

    stop_bot || true

    log "Остановка Ignite 3.1.0..."
    pkill -f "ignite-runner-3.1.0.jar" || pkill -f "ignite3db" || true
    sleep 5

    log "Восстановление work-директории из снапшота..."
    rm -rf "$IGNITE_WORK_DIR"
    cp -r "$WORK_SNAPSHOT" "$IGNITE_WORK_DIR"

    log "Запуск Ignite 3.0.0..."
    nohup "$IGNITE_DIR/bin/ignite3db" \
        --config-path "$IGNITE_DIR/etc/ignite-config.conf" \
        --work-dir "$IGNITE_WORK_DIR" \
        --node-name defaultNode \
        > "$IGNITE_DIR/log/ignite.log" 2>&1 &

    sleep 15
    start_bot || true
    warn "Откат выполнен. Проверьте работу бота."
}

# ── MAIN ──────────────────────────────────────────────────────────────────────
MODE="${1:-full}"

case "$MODE" in
    full)
        log "=== Начало полной миграции Ignite 3.0.0 → 3.1.0 ==="
        check_deps
        export_data
        stop_bot
        snapshot_work
        stop_ignite_300
        install_ignite_310
        start_ignite_310
        init_cluster_if_needed
        import_data
        start_bot
        verify_data
        log "=== Миграция завершена успешно ==="
        log "Backup: $BACKUP_DIR"
        log "Work snapshot: $WORK_SNAPSHOT"
        ;;
    export-only)
        check_deps
        export_data
        ;;
    import-only)
        [[ -n "${2:-}" ]] || die "Укажите путь к backup-директории: ./migrate-ignite.sh import-only /backup/ignite-migration-XXXXXX"
        BACKUP_DIR="$2"
        check_deps
        import_data
        ;;
    verify-only)
        [[ -n "${2:-}" ]] || die "Укажите путь к backup-директории: ./migrate-ignite.sh verify-only /backup/ignite-migration-XXXXXX"
        BACKUP_DIR="$2"
        check_deps
        verify_data
        ;;
    rollback)
        [[ -n "${2:-}" ]] || die "Укажите путь к work-снапшоту: ./migrate-ignite.sh rollback /opt/ignite3/work-snapshot-XXXXXX"
        WORK_SNAPSHOT="$2"
        rollback
        ;;
    *)
        echo "Использование: $0 [full|export-only|import-only <dir>|verify-only <dir>|rollback <snapshot>]"
        exit 1
        ;;
esac
