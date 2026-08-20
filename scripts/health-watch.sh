#!/usr/bin/env bash
#
# Notice when a company's API stops being ready, between deploys.
#
# The release pipeline already gates on readiness — but that only watches for the ten minutes after a
# push. This is the rest of the time: a database that went away at 3am, a disk that filled, a restart
# loop after an OOM.
#
# Deliberately small. It asks the same readiness endpoint the deploy gates on, and on a run of
# failures it says so once — to the journal always, and to a webhook if one is configured. It does not
# try to fix anything: a watchdog that restarts things hides the fault it was meant to report.
#
#   health-watch.sh --service jatelo --port 4451 [--failures 3] [--webhook https://…]
#
set -euo pipefail

SERVICE=""; PORT=""; NEEDED=3; WEBHOOK="${HEALTH_WEBHOOK:-}"
STATE_DIR=/var/lib/health-watch

while [[ $# -gt 0 ]]; do
  case "$1" in
    --service) SERVICE="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --failures) NEEDED="$2"; shift 2 ;;
    --webhook) WEBHOOK="$2"; shift 2 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done
[[ -n "$SERVICE" && -n "$PORT" ]] || { echo "usage: health-watch.sh --service <name> --port <actuator port>" >&2; exit 2; }

mkdir -p "$STATE_DIR"
STATE="$STATE_DIR/$SERVICE.failures"
COUNT=$(cat "$STATE" 2>/dev/null || echo 0)

CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:$PORT/actuator/health/readiness" || true)

if [[ "$CODE" == "200" ]]; then
  # Recovery is worth one line, but only if it had actually failed.
  if [[ "$COUNT" -ge "$NEEDED" ]]; then
    logger -t health-watch "$SERVICE is ready again after $COUNT failed checks"
    [[ -n "$WEBHOOK" ]] && curl -fsS -m 10 -X POST -H 'Content-Type: application/json' \
      -d "{\"service\":\"$SERVICE\",\"state\":\"recovered\",\"afterFailures\":$COUNT}" "$WEBHOOK" >/dev/null || true
  fi
  echo 0 > "$STATE"
  exit 0
fi

COUNT=$((COUNT + 1))
echo "$COUNT" > "$STATE"
logger -t health-watch "$SERVICE readiness answered '$CODE' (failure $COUNT of $NEEDED before alerting)"

# One alert per outage, not one per check: an inbox with forty copies of the same fault is an inbox
# nobody reads.
if [[ "$COUNT" -eq "$NEEDED" ]]; then
  DETAIL=$(curl -s -m 10 "http://127.0.0.1:$PORT/actuator/health/readiness" 2>/dev/null | head -c 400 || echo "(no answer)")
  logger -t health-watch "ALERT $SERVICE has been unready for $COUNT checks: $DETAIL"
  [[ -n "$WEBHOOK" ]] && curl -fsS -m 10 -X POST -H 'Content-Type: application/json' \
    -d "{\"service\":\"$SERVICE\",\"state\":\"unready\",\"checks\":$COUNT,\"detail\":$(printf '%s' "$DETAIL" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))')}" \
    "$WEBHOOK" >/dev/null || true
fi
exit 1
