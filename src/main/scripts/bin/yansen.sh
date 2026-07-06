#!/bin/bash
#
# Yansen Agent Server startup script
#

set -o errexit
set -o nounset

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

LIB_DIR="$BASE_DIR/lib"
CONF_DIR="$BASE_DIR/conf"
LOG_DIR="$BASE_DIR/logs"
STORE_DIR="$BASE_DIR/store"

mkdir -p "$LOG_DIR" "$STORE_DIR"

if [ -f "$SCRIPT_DIR/yansen.env" ]; then
    set -a
    # shellcheck source=/dev/null
    source "$SCRIPT_DIR/yansen.env"
    set +a
fi

MAIN_CLASS="${MAIN_CLASS:-com.glodon.mordor.yansen.AgentMain}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

YANSEN_CONFIG_FILE="${YANSEN_CONFIG_FILE:-$CONF_DIR/yansen.yml}"
YANSEN_WORKSPACE="${YANSEN_WORKSPACE:-$BASE_DIR/workspace}"
YANSEN_SQLITE_PATH="${YANSEN_SQLITE_PATH:-$STORE_DIR/yansen.db}"

# model_config / agent_config in SQLite may use ${MINIMAX_API_KEY:}, ${YANSEN_WORKSPACE:...}, etc.
export MINIMAX_API_KEY="${MINIMAX_API_KEY:-${API_KEY:-}}"

mkdir -p "$(dirname "$YANSEN_SQLITE_PATH")" "$YANSEN_WORKSPACE"

CP="$LIB_DIR/*"

export YANSEN_CONFIG_FILE YANSEN_WORKSPACE YANSEN_SQLITE_PATH MINIMAX_API_KEY

exec java $JAVA_OPTS \
    -Dyansen.home="$BASE_DIR" \
    -Dyansen.logDir="$LOG_DIR" \
    -cp "$CP" \
    "$MAIN_CLASS" "$@"
