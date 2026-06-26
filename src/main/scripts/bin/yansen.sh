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

mkdir -p "$LOG_DIR"

if [ -f "$SCRIPT_DIR/yansen.env" ]; then
    set -a
    source "$SCRIPT_DIR/yansen.env"
    set +a
fi

MAIN_CLASS="${MAIN_CLASS:-com.glodon.mordor.yansen.AgentMain}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

YANSEN_CONFIG_FILE="${YANSEN_CONFIG_FILE:-$CONF_DIR/yansen.yml}"
YANSEN_SYSTEM_PROMPT="${YANSEN_SYSTEM_PROMPT:-$CONF_DIR/system-prompt.md}"
YANSEN_WORKSPACE="${YANSEN_WORKSPACE:-$BASE_DIR/workspace}"

CP="$LIB_DIR/*"

export YANSEN_CONFIG_FILE YANSEN_SYSTEM_PROMPT YANSEN_WORKSPACE

exec java $JAVA_OPTS \
    -Dyansen.home="$BASE_DIR" \
    -Dyansen.logDir="$LOG_DIR" \
    -cp "$CP" \
    "$MAIN_CLASS" "$@"
