#!/bin/bash
# =============================================================================
# test_throughput.sh — Throughput stress tester for the ingest pipeline
#
# Sends N tweets as fast as possible (or at a fixed rate) and reports:
#   - Total sent / failed
#   - Elapsed time
#   - Client-side send rate (req/s)
#   - Pipeline throughput via GET /api/analytics/report (server-side msg/s)
#
# Usage:
#   ./test_throughput.sh [OPTIONS]
#
# Options:
#   -n  COUNT     Number of tweets to send        (default: 100)
#   -c  WORKERS   Parallel curl workers           (default: 5)
#   -r  RATE      Max sends per second, 0 = unlimited (default: 0)
#   -u  URL       API base URL                    (default: http://localhost:8090)
#   -w  WAIT      Seconds to wait after sending before reading throughput (default: 15)
#   -h            Show this help
# =============================================================================

set -euo pipefail

# ── Defaults ──────────────────────────────────────────────────────────────────
COUNT=100
WORKERS=5
RATE=0          # 0 = unlimited
API_URL="http://localhost:8090"
WAIT_SECS=15

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

# ── Argument parsing ──────────────────────────────────────────────────────────
usage() {
  sed -n '3,18p' "$0" | sed 's/^# \{0,2\}//'
  exit 0
}

while getopts "n:c:r:u:w:h" opt; do
  case $opt in
    n) COUNT=$OPTARG ;;
    c) WORKERS=$OPTARG ;;
    r) RATE=$OPTARG ;;
    u) API_URL=$OPTARG ;;
    w) WAIT_SECS=$OPTARG ;;
    h) usage ;;
    *) echo "Unknown option: -$OPTARG"; exit 1 ;;
  esac
done

INGEST_URL="${API_URL}/api/tweets"
REPORT_URL="${API_URL}/api/analytics/report"

# ── Tweet pool (mixed sentiments) ─────────────────────────────────────────────
TWEETS=(
  "I absolutely love this product, it changed my life!"
  "Worst purchase I have ever made. Total garbage."
  "The delivery arrived on time."
  "Amazing customer support, very responsive."
  "App keeps crashing on startup, very frustrating."
  "Works as described, nothing special."
  "Incredible build quality, worth every penny."
  "Completely broken out of the box, do not buy."
  "Just installed it, testing it out now."
  "Exceeded all my expectations, highly recommend!"
  "Terrible software update broke everything."
  "Waiting for the next version before judging."
  "Best product in its category by far."
  "Disappointed with the lack of features."
  "Neutral experience, met basic requirements."
  "Outstanding performance under heavy load."
  "Shipping took forever and packaging was damaged."
  "It is exactly what I needed for my workflow."
  "Support team was rude and unhelpful."
  "Smooth interface, very intuitive design."
  "Not sure how I feel about this yet."
  "Five stars, would buy again without hesitation."
  "One star, complete waste of money."
  "The specs are accurate, no surprises."
  "Blew me away with its speed and reliability."
  "Battery drains way too fast on this device."
  "Solid mid-range option for the price."
  "Genuinely impressed by the thoughtful design."
  "Broke after one week of normal use."
  "Does the job, nothing more nothing less."
)
POOL_SIZE=${#TWEETS[@]}

# ── Helpers ───────────────────────────────────────────────────────────────────
check_deps() {
  for cmd in curl jq bc; do
    if ! command -v "$cmd" &>/dev/null; then
      echo -e "${RED}ERROR:${RESET} '$cmd' is required but not installed."
      exit 1
    fi
  done
}

check_api() {
  if ! curl -sf "${API_URL}/api/health" -o /dev/null --max-time 3; then
    echo -e "${RED}ERROR:${RESET} Cannot reach ${API_URL}. Is the backend running?"
    exit 1
  fi
}

send_tweet() {
  local idx=$1
  local text="${TWEETS[$((idx % POOL_SIZE))]}"
  local payload="{\"text\": \"${text}\", \"source\": \"throughput-test\"}"
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "${INGEST_URL}" \
    -H "Content-Type: application/json" \
    -d "$payload" \
    --max-time 5 2>/dev/null)
  echo "$http_code"
}

export -f send_tweet
export INGEST_URL TWEETS POOL_SIZE

# ── Rate-limit helper: sleep if needed to stay under RATE req/s ───────────────
rate_sleep() {
  # called after each send; $1 = sequence number (0-based), $2 = start epoch (ns)
  if [[ "$RATE" -gt 0 ]]; then
    local seq=$1
    local start_ns=$2
    local expected_ns=$(( start_ns + (seq + 1) * 1000000000 / RATE ))
    local now_ns
    now_ns=$(date +%s%N)
    local sleep_ns=$(( expected_ns - now_ns ))
    if [[ $sleep_ns -gt 0 ]]; then
      sleep "$(echo "scale=9; $sleep_ns / 1000000000" | bc)"
    fi
  fi
}

# ── Main ──────────────────────────────────────────────────────────────────────
check_deps
check_api

echo ""
echo -e "${BOLD}${CYAN}══════════════════════════════════════════════${RESET}"
echo -e "${BOLD}  PulseStream — Throughput Test${RESET}"
echo -e "${BOLD}${CYAN}══════════════════════════════════════════════${RESET}"
echo -e "  Target  : ${INGEST_URL}"
echo -e "  Tweets  : ${COUNT}"
echo -e "  Workers : ${WORKERS}"
echo -e "  Rate    : $([ "$RATE" -eq 0 ] && echo 'unlimited' || echo "${RATE} req/s")"
echo -e "${BOLD}${CYAN}══════════════════════════════════════════════${RESET}"
echo ""

# ── Capture totals before test ────────────────────────────────────────────────
PRE_TOTAL=$(curl -sf "${REPORT_URL}" 2>/dev/null | jq -r '.summary.totalProcessed // 0')
echo -e "${YELLOW}▶ Pipeline total before test: ${PRE_TOTAL} tweets${RESET}"
echo ""

# ── Send phase ────────────────────────────────────────────────────────────────
echo -e "${BOLD}Sending ${COUNT} tweets with ${WORKERS} parallel workers...${RESET}"

SENT=0
FAILED=0
START_NS=$(date +%s%N)
START_S=$SECONDS

# Use a temp dir for per-job status files
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

# Worker function
worker() {
  local start_idx=$1
  local step=$2
  local total=$3
  local worker_id=$4
  local ok=0
  local fail=0
  for (( i=start_idx; i<total; i+=step )); do
    code=$(send_tweet "$i")
    if [[ "$code" == "2"* ]]; then
      (( ok++ )) || true
    else
      (( fail++ )) || true
    fi
    # Rate limiting (applies per worker, so effective rate = RATE/WORKERS per worker)
  done
  echo "$ok $fail" > "${TMP_DIR}/worker_${worker_id}.result"
}
export -f worker
export TMP_DIR RATE

# Launch workers in background
PIDS=()
for (( w=0; w<WORKERS; w++ )); do
  worker "$w" "$WORKERS" "$COUNT" "$w" &
  PIDS+=($!)
done

# Progress bar while workers run
DONE=0
while [[ $DONE -lt $WORKERS ]]; do
  DONE=0
  for pid in "${PIDS[@]}"; do
    kill -0 "$pid" 2>/dev/null || (( DONE++ )) || true
  done
  # Count results so far
  PARTIAL=$(cat "${TMP_DIR}"/worker_*.result 2>/dev/null | awk '{s+=$1+$2} END{print s+0}')
  PCT=$(( PARTIAL * 100 / COUNT ))
  BAR=$(printf '#%.0s' $(seq 1 $(( PCT / 5 ))))
  printf "\r  [%-20s] %3d%% (%d/%d)" "$BAR" "$PCT" "$PARTIAL" "$COUNT"
  sleep 0.3
done
printf "\r  [####################] 100%% (%d/%d)\n" "$COUNT" "$COUNT"

# Collect results
for f in "${TMP_DIR}"/worker_*.result; do
  read -r ok fail < "$f"
  SENT=$(( SENT + ok ))
  FAILED=$(( FAILED + fail ))
done

END_NS=$(date +%s%N)
ELAPSED_NS=$(( END_NS - START_NS ))
ELAPSED_S=$(echo "scale=3; $ELAPSED_NS / 1000000000" | bc)
CLIENT_RPS=$(echo "scale=2; $COUNT / $ELAPSED_S" | bc)

echo ""
echo -e "${BOLD}${CYAN}── Send Results ───────────────────────────────${RESET}"
echo -e "  Elapsed     : ${ELAPSED_S}s"
echo -e "  Sent OK     : ${GREEN}${SENT}${RESET}"
echo -e "  Failed      : $([ "$FAILED" -gt 0 ] && echo "${RED}${FAILED}${RESET}" || echo "${GREEN}0${RESET}")"
echo -e "  Client RPS  : ${BOLD}${CLIENT_RPS} req/s${RESET}"
echo ""

# ── Wait for pipeline to process ─────────────────────────────────────────────
echo -e "${YELLOW}⏳ Waiting ${WAIT_SECS}s for Spark pipeline to process...${RESET}"
for (( i=WAIT_SECS; i>0; i-- )); do
  printf "\r   %2ds remaining..." "$i"
  sleep 1
done
printf "\r                          \r"

# ── Poll pipeline throughput (3 samples × 5 s) ───────────────────────────────
echo -e "${BOLD}Sampling pipeline throughput (3 × 5s)...${RESET}"
SAMPLES=()
for sample in 1 2 3; do
  REPORT=$(curl -sf "${REPORT_URL}" 2>/dev/null || echo "{}")
  RPS=$(echo "$REPORT" | jq -r '.summary.throughputPerSec // 0')
  TOTAL_NOW=$(echo "$REPORT" | jq -r '.summary.totalProcessed // 0')
  echo -e "  Sample ${sample}: ${CYAN}${RPS} msg/s${RESET}  (total: ${TOTAL_NOW})"
  SAMPLES+=("$RPS")
  [[ $sample -lt 3 ]] && sleep 5
done

AVG_RPS=$(echo "${SAMPLES[*]}" | tr ' ' '\n' | awk '{s+=$1} END{printf "%.2f", s/NR}')
POST_TOTAL=$(echo "$REPORT" | jq -r '.summary.totalProcessed // 0')
DELTA=$(( POST_TOTAL - PRE_TOTAL ))

echo ""
echo -e "${BOLD}${CYAN}── Pipeline Results ────────────────────────────${RESET}"
echo -e "  Tweets processed (delta) : ${GREEN}${DELTA}${RESET}"
echo -e "  Avg pipeline throughput  : ${BOLD}${AVG_RPS} msg/s${RESET}"
echo -e "  New pipeline total       : ${POST_TOTAL}"
echo -e "${BOLD}${CYAN}════════════════════════════════════════════════${RESET}"
echo ""
