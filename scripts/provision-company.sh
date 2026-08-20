#!/usr/bin/env bash
#
# Provision one company's API on a host: database, user, config, storage, service.
#
# The product is one codebase serving several tour companies. Everything that differs between them is
# either a row in that company's database (identity, brand, features) or a line in the file this
# script writes. Nothing is forked.
#
# Run it ON the target host as root.
#
#   provision-company.sh --id jatelo --name "Jatelo African Travels" \
#       --api-url https://api.jateloafricantravels.com \
#       --panel-url https://management.jateloafricantravels.com \
#       --admin-email ops@jateloafricantravels.com [--port 4460] [--stable-ids] [--dry-run]
#
# Idempotent by design: every step checks before it acts, and it NEVER overwrites an existing
# secrets file — re-running against a live install must not rotate its database password out from
# under the running service. Steps already done are reported as such.
#
# What it deliberately does NOT do (a human must, and the summary says so):
#   DNS · TLS certificates · SMTP credentials · the SPA build and its nginx site · the GitHub
#   Environment · filling in Settings → Company.
#
set -euo pipefail

ID=""; NAME=""; API_URL=""; PANEL_URL=""; ADMIN_EMAIL=""
PORT=""; MGMT_PORT=""; DRY_RUN=0
JAR_SOURCE=""
SMTP_HOST=""; SMTP_PORT="587"; SMTP_USER=""; SMTP_PASSWORD=""; SMTP_FROM=""
STABLE_IDS=0

usage() { sed -n '3,25p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --id) ID="$2"; shift 2 ;;
    --name) NAME="$2"; shift 2 ;;
    --api-url) API_URL="$2"; shift 2 ;;
    --panel-url) PANEL_URL="$2"; shift 2 ;;
    --admin-email) ADMIN_EMAIL="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --management-port) MGMT_PORT="$2"; shift 2 ;;
    --jar) JAR_SOURCE="$2"; shift 2 ;;
    --smtp-host) SMTP_HOST="$2"; shift 2 ;;
    --smtp-port) SMTP_PORT="$2"; shift 2 ;;
    --smtp-user) SMTP_USER="$2"; shift 2 ;;
    --smtp-password) SMTP_PASSWORD="$2"; shift 2 ;;
    --smtp-from) SMTP_FROM="$2"; shift 2 ;;
    --stable-ids) STABLE_IDS=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage ;;
    *) echo "unknown option: $1" >&2; usage ;;
  esac
done

[[ -n "$ID" && -n "$NAME" && -n "$API_URL" && -n "$PANEL_URL" ]] || usage
[[ "$ID" =~ ^[a-z][a-z0-9-]{1,30}$ ]] || { echo "--id must be a lowercase slug" >&2; exit 1; }

PORT="${PORT:-4450}"
MGMT_PORT="${MGMT_PORT:-$((PORT + 1))}"

DB_NAME="app_${ID//-/_}"
DB_USER="${ID//-/_}"
APP_DIR="/opt/$ID"
DATA_DIR="/srv/$ID/data"
BACKUP_DIR="/srv/$ID/backups"
ENV_FILE="/etc/$ID.env"
UNIT="/etc/systemd/system/$ID.service"
# The company's own service account, distinct from the account CI logs in with. Two reasons: a
# process and the pipeline that replaces it should not be the same identity, and on a host carrying
# more than one company /proc/<pid>/environ would otherwise hand one company's database password to
# the other. `deploy` remains the login CI uses.
SERVICE_USER="$ID"
DEPLOY_USER="deploy"

# The host's ~/.my.cnf may point at an application user with rights on one database — enough to read,
# not to create. Provisioning needs administrative access, so it asks for it explicitly instead of
# inheriting a config that will fail three steps in with "access denied".
MYSQL="mysql --defaults-file=/dev/null -u root"

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
info() { printf '  %s\n' "$*"; }
skip() { printf '  \033[2m· %s (already done)\033[0m\n' "$*"; }

run() {
  if (( DRY_RUN )); then printf '  \033[2m$ %s\033[0m\n' "$*"; else eval "$@"; fi
}

# A secret is generated on the host and never printed. --dry-run prints a placeholder so the output
# can be read and pasted into a review without leaking anything.
secret() {
  if (( DRY_RUN )); then echo "<generated-$1>"; else openssl rand -base64 "$2" | tr -d '/+=' | cut -c1-"$3"; fi
}

DRY_LABEL=""
(( DRY_RUN )) && DRY_LABEL=" (dry run — nothing will change)"
say "Provisioning '$NAME' as '$ID'$DRY_LABEL"
info "database      $DB_NAME (user $DB_USER)"
info "config        $ENV_FILE + $APP_DIR/application.properties"
info "storage       $DATA_DIR, backups in $BACKUP_DIR"
info "service       $ID.service on port $PORT (actuator $MGMT_PORT)"

# ---------------------------------------------------------------- 0. what is already here
say "0. Checking the host"
for port in "$PORT" "$MGMT_PORT"; do
  if ss -ltn 2>/dev/null | grep -q ":$port "; then
    echo "  !! port $port is already in use — pass --port to pick another" >&2
    (( DRY_RUN )) || exit 1
  fi
done
info "ports $PORT and $MGMT_PORT are free"
id -u "$SERVICE_USER" >/dev/null 2>&1 || {
  info "creating the service account '$SERVICE_USER' (runs the API, cannot log in)"
  run "useradd --system --shell /usr/sbin/nologin --home /nonexistent '$SERVICE_USER'"
}
id -u "$DEPLOY_USER" >/dev/null 2>&1 || {
  info "creating the deploy account '$DEPLOY_USER' (CI logs in as this; add its key afterwards)"
  run "useradd --system --shell /bin/bash --create-home '$DEPLOY_USER'"
}

# ---------------------------------------------------------------- 1. database
say "1. Database"
DB_PASSWORD=""
if $MYSQL -N -B -e "show databases like '$DB_NAME'" 2>/dev/null | grep -q "$DB_NAME"; then
  skip "database $DB_NAME exists"
else
  run "$MYSQL -e \"create database \\\`$DB_NAME\\\` character set utf8mb4 collate utf8mb4_unicode_ci\""
  info "created $DB_NAME"
fi

if [[ -f "$ENV_FILE" ]]; then
  skip "$ENV_FILE exists — keeping the password already in it"
else
  DB_PASSWORD="$(secret db 32 28)"
  run "$MYSQL -e \"create user if not exists '$DB_USER'@'localhost' identified by '\$DB_PASSWORD'\""
  run "$MYSQL -e \"grant all privileges on \\\`$DB_NAME\\\`.* to '$DB_USER'@'localhost'; flush privileges\""
  info "created the MySQL user $DB_USER with rights on $DB_NAME only"
fi

# ---------------------------------------------------------------- 2. storage
say "2. Storage"
for dir in "$APP_DIR" "$APP_DIR/releases" "$DATA_DIR" "$BACKUP_DIR"; do
  if [[ -d "$dir" ]]; then skip "$dir"; else run "mkdir -p '$dir'"; info "created $dir"; fi
done
# the release directory is replaced by CI; the storage is written by the service
run "chown -R '$DEPLOY_USER:$DEPLOY_USER' '$APP_DIR'"
run "chown -R '$SERVICE_USER:$SERVICE_USER' '/srv/$ID'"
run "chmod 755 '$APP_DIR'"
run "chmod 750 '/srv/$ID'"

# ---------------------------------------------------------------- 3. secrets
say "3. Secrets"
if [[ -f "$ENV_FILE" ]]; then
  skip "$ENV_FILE — never rewritten, or a restart would lose the running install's credentials"
else
  # AES-256 expects base64 of exactly 32 bytes. The generic generator strips +/= to keep values
  # shell-safe, which produces a string that is NOT decodable — the first boot then fails with
  # "Encryption failed" while saving the mail account.
  ENCRYPTION_KEY="$( (( DRY_RUN )) && echo '<generated-key>' || openssl rand -base64 32 )"
  #
  # The id salt is left UNSET by default, which means the app derives a new one at every boot.
  #
  # That is the house choice, deliberately: an identifier that escapes into a referrer header, a proxy
  # log or somebody's browser history stops resolving at the next restart. The cost is that no
  # external reference to a record survives a deploy — a saved deep link, or a saved filter naming a
  # record, goes stale. --stable-ids trades that the other way and fixes a salt per company (never
  # shared, or one company's ids would be valid shapes in another's).
  #
  ID_SALT=""
  (( STABLE_IDS )) && ID_SALT="$(secret salt 48 40)"
  run "install -m 600 -o root -g root /dev/null '$ENV_FILE'"
  run "cat > '$ENV_FILE' <<EOF
# $NAME — written by provision-company.sh. Root-only: the service reads it, nobody prints it.
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/$DB_NAME?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000
SPRING_DATASOURCE_USERNAME=$DB_USER
SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD
MYSQL_DATABASE=$DB_NAME
EMAIL_ENCRYPTION_KEY=$ENCRYPTION_KEY
SECURITY_IDOBFUSCATOR_SALT=$ID_SALT
APP_DATA_DIR=$DATA_DIR
APP_BACKUP_DIR=$BACKUP_DIR
APP_BOOTSTRAP_ADMIN_EMAIL=${ADMIN_EMAIL:-}
# The mail account the first boot writes into the database. Without it the activation email for the
# first account cannot be sent, and nobody can sign in to configure mail — the provisioning deadlock.
APP_BOOTSTRAP_SMTP_HOST=${SMTP_HOST:-}
APP_BOOTSTRAP_SMTP_PORT=${SMTP_PORT:-587}
APP_BOOTSTRAP_SMTP_USERNAME=${SMTP_USER:-}
APP_BOOTSTRAP_SMTP_PASSWORD=${SMTP_PASSWORD:-}
APP_BOOTSTRAP_SMTP_FROM=${SMTP_FROM:-$ADMIN_EMAIL}
# Where this company's own newsletter signups, booking inquiries, contact messages and backup
# reports are sent. There is deliberately NO default: the shared one used to name a real company, so
# a new install quietly addressed its notifications to somebody else's mailbox.
NOTIFICATION_EMAILS=${SMTP_FROM:-$ADMIN_EMAIL}
EOF"
  info "wrote $ENV_FILE (600 root:root) — database password and encryption key"
  if (( STABLE_IDS )); then
    info "ids are FIXED for this company: saved links survive a restart"
  else
    info "ids rotate on every restart (the default): a leaked identifier expires, saved links do not survive"
  fi
fi

# ---------------------------------------------------------------- 4. non-secret config
say "4. Configuration"
if [[ -f "$APP_DIR/application.properties" ]]; then
  skip "$APP_DIR/application.properties — edit it in place rather than re-provisioning"
else
  run "cat > '$APP_DIR/application.properties' <<EOF
# $NAME — per-company overrides. Secrets live in $ENV_FILE, not here.
server.port=$PORT
management.server.port=$MGMT_PORT

app.company.name=$NAME
app.base.url=$API_URL
app.management.base.url=$PANEL_URL

# One company's seeded blog posts and FAQs are not this company's content.
app.seed.demo-content.enabled=false

# Everything else — identity, contact details, brand, which features exist — is DATA, edited in the
# panel at Settings → Company and Settings → Features. Nothing about this company is compiled in.
EOF"
  run "chown '$DEPLOY_USER:$DEPLOY_USER' '$APP_DIR/application.properties'"
  info "wrote $APP_DIR/application.properties"
fi

# ---------------------------------------------------------------- 5. the service
say "5. Service"
if [[ -f "$UNIT" ]]; then
  skip "$UNIT"
else
  run "cat > '$UNIT' <<EOF
[Unit]
Description=$NAME — API
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_USER
WorkingDirectory=$APP_DIR
EnvironmentFile=$ENV_FILE
ExecStart=/usr/bin/java -Xms256m -Xmx640m -jar $APP_DIR/app.jar --spring.config.additional-location=file:$APP_DIR/application.properties
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

# The service reads one secrets file and writes two directories. Nothing else.
NoNewPrivileges=true
ProtectSystem=full
ProtectHome=true
PrivateTmp=true
ReadWritePaths=$APP_DIR /srv/$ID

[Install]
WantedBy=multi-user.target
EOF"
  run "systemctl daemon-reload"
  run "systemctl enable '$ID.service' >/dev/null"
  info "wrote $UNIT and enabled it"
fi

if [[ -n "$JAR_SOURCE" ]]; then
  if [[ -f "$JAR_SOURCE" ]]; then
    run "install -o '$SERVICE_USER' -g '$SERVICE_USER' -m 644 '$JAR_SOURCE' '$APP_DIR/app.jar'"
    info "installed $JAR_SOURCE as $APP_DIR/app.jar"
  else
    echo "  !! --jar $JAR_SOURCE does not exist" >&2
    (( DRY_RUN )) || exit 1
  fi
else
  info "no --jar given: CI will deliver $APP_DIR/app.jar"
fi

# ---------------------------------------------------------------- 6. first boot
say "6. First boot"
if [[ -f "$APP_DIR/app.jar" ]]; then
  run "systemctl restart '$ID.service'"
  info "started; readiness is http://127.0.0.1:$MGMT_PORT/actuator/health/readiness"
  info "the first boot runs the migrations and ~33 seeding runners — allow a few minutes"
else
  info "nothing to start yet: no $APP_DIR/app.jar"
fi

# ---------------------------------------------------------------- what a person must still do
cat <<SUMMARY

$(printf '\033[1m%s\033[0m' "Provisioned. What still needs a human:")

  1. DNS       point $(echo "$API_URL" | sed 's|https\?://||') and
               $(echo "$PANEL_URL" | sed 's|https\?://||') at this host
  2. nginx     a site proxying the API host to 127.0.0.1:$PORT, and one serving the
               panel's docroot as a SPA (try_files … /index.html)
  3. TLS       certbot for both hostnames
  4. SMTP      ${SMTP_HOST:+configured at provisioning ($SMTP_HOST) — check the log line saying the
               mail account was created}${SMTP_HOST:-add the mail account in the panel; until then NO email can be sent,
               including the first account's activation link, so nobody can sign in. Re-run with
               --smtp-host/--smtp-user/--smtp-password/--smtp-from and restart the service}
  5. CI        a GitHub Environment for '$ID' holding its host, key and VITE_API_BASE_URL,
               so one merge deploys this company along with the others
  6. Identity  sign in and fill Settings → Company: name, TIN, emails, phones, addresses, links,
               and the seven brand files. Every document reads from there.
  7. Features  Settings → Features: switch off what this company did not buy

  The first account is $ADMIN_EMAIL — it arrives by activation email and has no password until
  that link is followed. If mail was not working at first boot, the log says so; fix mail and
  restart, and it will be created then.

SUMMARY
