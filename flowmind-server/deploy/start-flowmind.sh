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
PUBLIC_API_BASE_URL="${PUBLIC_API_BASE_URL:-http://$SERVER_HOST:8080}"
WEB_PORT="${WEB_PORT:-5173}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-flowmind}"
DB_USERNAME="${DB_USERNAME:-flowmind}"
DB_PASSWORD="${DB_PASSWORD:-flowmind123}"
QDRANT_HOST="${QDRANT_HOST:-127.0.0.1}"
QDRANT_PORT="${QDRANT_PORT:-6333}"
UPLOAD_DIR="${UPLOAD_DIR:-$APP_DIR/flowmind-server/data/uploads}"
JAVA_OPTS="${JAVA_OPTS:- -Xms256m -Xmx768m}"
RAG_RATE_LIMIT_ENABLED="${RAG_RATE_LIMIT_ENABLED:-false}"

LOG_DIR="$APP_DIR/logs"
RUN_DIR="$APP_DIR/run"
mkdir -p "$LOG_DIR" "$RUN_DIR" "$UPLOAD_DIR"

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  else
    docker-compose "$@"
  fi
}

wait_port() {
  local host="$1"
  local port="$2"
  local name="$3"
  for _ in {1..60}; do
    if command -v nc >/dev/null 2>&1 && nc -z "$host" "$port" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "$name did not become ready on $host:$port" >&2
  return 1
}

if [[ -z "${DASHSCOPE_API_KEY:-}" ]]; then
  echo "WARN: DASHSCOPE_API_KEY is empty. RAG/chat calls may fail." >&2
fi

cd "$APP_DIR"

if ! docker ps --format '{{.Names}}' | grep -qx 'flowmind-postgres'; then
  rm -f "$APP_DIR/data/postgres/postmaster.pid"
fi

echo "Starting Postgres and Qdrant..."
APP_DIR="$APP_DIR" DB_NAME="$DB_NAME" DB_USERNAME="$DB_USERNAME" DB_PASSWORD="$DB_PASSWORD" \
  DB_PORT="$DB_PORT" QDRANT_PORT="$QDRANT_PORT" \
  compose -f "$SCRIPT_DIR/docker-compose.infra.yml" up -d

wait_port "$DB_HOST" "$DB_PORT" "Postgres"
wait_port "$QDRANT_HOST" "$QDRANT_PORT" "Qdrant"

echo "Building backend..."
cd "$APP_DIR/flowmind-server"
chmod +x ./mvnw
./mvnw -q -DskipTests package

BACKEND_JAR="$(ls -1 target/*.jar | grep -v 'original-' | head -1)"
if [[ -z "$BACKEND_JAR" ]]; then
  echo "Backend jar not found" >&2
  exit 1
fi

if [[ -f "$RUN_DIR/backend.pid" ]] && kill -0 "$(cat "$RUN_DIR/backend.pid")" >/dev/null 2>&1; then
  echo "Stopping existing backend..."
  kill "$(cat "$RUN_DIR/backend.pid")" || true
  sleep 2
fi

echo "Starting backend on port $BACKEND_PORT..."
nohup env \
  SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
  SERVER_PORT="$BACKEND_PORT" \
  DB_HOST="$DB_HOST" \
  DB_PORT="$DB_PORT" \
  DB_NAME="$DB_NAME" \
  DB_USERNAME="$DB_USERNAME" \
  DB_PASSWORD="$DB_PASSWORD" \
  QDRANT_HOST="$QDRANT_HOST" \
  QDRANT_PORT="$QDRANT_PORT" \
  UPLOAD_DIR="$UPLOAD_DIR" \
  DASHSCOPE_API_KEY="${DASHSCOPE_API_KEY:-}" \
  RAG_RATE_LIMIT_ENABLED="$RAG_RATE_LIMIT_ENABLED" \
  java $JAVA_OPTS -jar "$BACKEND_JAR" \
  > "$LOG_DIR/backend.log" 2>&1 &
echo $! > "$RUN_DIR/backend.pid"

echo "Building frontend..."
cd "$APP_DIR/flowmind-web"
if [[ -f package-lock.json ]]; then
  npm ci
else
  npm install
fi
VITE_API_BASE_URL="$PUBLIC_API_BASE_URL" npm run build

if [[ -f "$RUN_DIR/frontend.pid" ]] && kill -0 "$(cat "$RUN_DIR/frontend.pid")" >/dev/null 2>&1; then
  echo "Stopping existing frontend..."
  kill "$(cat "$RUN_DIR/frontend.pid")" || true
  sleep 2
fi

echo "Starting frontend on port $WEB_PORT..."
nohup npm run preview -- --host 0.0.0.0 --port "$WEB_PORT" \
  > "$LOG_DIR/frontend.log" 2>&1 &
echo $! > "$RUN_DIR/frontend.pid"

echo "FlowMind AI is starting."
echo "Frontend: http://$SERVER_HOST:$WEB_PORT"
echo "Backend health: http://$SERVER_HOST:$BACKEND_PORT/api/health"
echo "Logs: $LOG_DIR/backend.log, $LOG_DIR/frontend.log"
