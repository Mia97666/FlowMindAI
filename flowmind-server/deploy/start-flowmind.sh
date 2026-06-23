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
PUBLIC_API_BASE_URL="${PUBLIC_API_BASE_URL:-}"
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

JAVA_BIN="${JAVA_BIN:-java}"
JAVA21_HOME="$(ls -d /usr/lib/jvm/java-21-openjdk* 2>/dev/null | head -1 || true)"
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
elif [[ -n "$JAVA21_HOME" && -x "$JAVA21_HOME/bin/java" ]]; then
  export JAVA_HOME="$JAVA21_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
  JAVA_BIN="$JAVA_HOME/bin/java"
fi

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
    if timeout 1 bash -c "cat < /dev/null > /dev/tcp/$host/$port" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "$name did not become ready on $host:$port" >&2
  return 1
}

apply_sql_migrations() {
  local migration_dir="$APP_DIR/flowmind-server/src/main/resources/db/migration"
  if [[ ! -d "$migration_dir" ]]; then
    return 0
  fi

  shopt -s nullglob
  local migration_files=("$migration_dir"/*.sql)
  shopt -u nullglob
  if [[ ${#migration_files[@]} -eq 0 ]]; then
    return 0
  fi

  echo "Applying database migrations..."
  for sql_file in "${migration_files[@]}"; do
    echo "Applying $(basename "$sql_file")"
    docker exec -i flowmind-postgres \
      psql -v ON_ERROR_STOP=1 -U "$DB_USERNAME" -d "$DB_NAME" \
      < "$sql_file"
  done
}

configure_nginx() {
  if ! command -v nginx >/dev/null 2>&1; then
    echo "WARN: nginx is not installed. Built frontend files are in $APP_DIR/flowmind-web/dist." >&2
    return 0
  fi
  if [[ ! -d /etc/nginx/conf.d || ! -w /etc/nginx/conf.d ]]; then
    echo "WARN: /etc/nginx/conf.d is not writable. Please install $SCRIPT_DIR/nginx-flowmind.conf manually." >&2
    return 0
  fi

  echo "Configuring nginx static frontend..."
  cat > /etc/nginx/conf.d/flowmind.conf <<NGINX
server {
    listen 80;
    server_name $SERVER_HOST;
    root $APP_DIR/flowmind-web/dist;
    index index.html;

    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_comp_level 5;
    gzip_types
        text/plain
        text/css
        application/json
        application/javascript
        text/xml
        application/xml
        application/xml+rss
        image/svg+xml;

    location /api/ {
        proxy_pass http://127.0.0.1:$BACKEND_PORT/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /actuator/ {
        proxy_pass http://127.0.0.1:$BACKEND_PORT/actuator/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /assets/ {
        try_files \$uri =404;
        expires 1y;
        add_header Cache-Control "public, max-age=31536000, immutable";
        access_log off;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
        add_header Cache-Control "no-cache";
    }
}
NGINX
  nginx -t
  if command -v systemctl >/dev/null 2>&1; then
    systemctl enable nginx >/dev/null 2>&1 || true
    systemctl restart nginx
  else
    nginx -s reload || nginx
  fi
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
apply_sql_migrations

echo "Building backend..."
cd "$APP_DIR/flowmind-server"
if [[ -f ./.mvn/wrapper/maven-wrapper.properties ]]; then
  chmod +x ./mvnw
  ./mvnw -q -DskipTests package
else
  mvn -q -DskipTests package
fi

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
  "$JAVA_BIN" $JAVA_OPTS -jar "$BACKEND_JAR" \
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
  echo "Stopping existing vite preview frontend..."
  kill "$(cat "$RUN_DIR/frontend.pid")" || true
  sleep 2
fi
rm -f "$RUN_DIR/frontend.pid"

configure_nginx

echo "FlowMind AI is starting."
echo "Public site: $PUBLIC_SITE_URL"
echo "Frontend: $PUBLIC_SITE_URL"
echo "Backend health: http://$SERVER_HOST:$BACKEND_PORT/api/health"
echo "Logs: $LOG_DIR/backend.log"
