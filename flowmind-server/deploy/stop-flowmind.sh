#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_APP_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/flowmind.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

APP_DIR="${APP_DIR:-$DEFAULT_APP_DIR}"
RUN_DIR="$APP_DIR/run"

stop_pid() {
  local name="$1"
  local pid_file="$2"
  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" >/dev/null 2>&1; then
    echo "Stopping $name..."
    kill "$(cat "$pid_file")" || true
    rm -f "$pid_file"
  else
    echo "$name is not running."
    rm -f "$pid_file"
  fi
}

stop_pid "frontend" "$RUN_DIR/frontend.pid"
stop_pid "backend" "$RUN_DIR/backend.pid"

echo "Postgres and Qdrant are left running. Stop them with:"
echo "docker compose -f $SCRIPT_DIR/docker-compose.infra.yml down"
