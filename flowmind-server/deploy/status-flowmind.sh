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
SERVER_HOST="${SERVER_HOST:-150.158.119.197}"
PUBLIC_SITE_URL="${PUBLIC_SITE_URL:-http://$SERVER_HOST}"
WEB_PORT="${WEB_PORT:-5173}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
RUN_DIR="$APP_DIR/run"

show_pid() {
  local name="$1"
  local pid_file="$2"
  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" >/dev/null 2>&1; then
    echo "$name: running (pid $(cat "$pid_file"))"
  else
    echo "$name: stopped"
  fi
}

show_pid "backend" "$RUN_DIR/backend.pid"
show_pid "frontend" "$RUN_DIR/frontend.pid"

if command -v docker >/dev/null 2>&1; then
  docker ps --filter "name=flowmind-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
fi

echo "Public site: $PUBLIC_SITE_URL"
echo "Frontend: http://$SERVER_HOST:$WEB_PORT"
echo "Backend health: http://$SERVER_HOST:$BACKEND_PORT/api/health"
