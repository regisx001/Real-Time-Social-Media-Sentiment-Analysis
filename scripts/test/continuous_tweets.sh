#!/bin/bash
# =============================================================================
# continuous_tweets.sh — Sends tweets at a gentle pace over 10 minutes
#
# Default: 1 tweet every 6 seconds → ~100 tweets / 10 min
# Designed to produce visible throughput in the dashboard without hammering
# the API.
#
# Usage:
#   ./continuous_tweets.sh [OPTIONS]
#
# Options:
#   -d  DURATION   Total run duration in seconds   (default: 600  = 10 min)
#   -i  INTERVAL   Seconds between each tweet       (default: 6)
#   -u  URL        API base URL                     (default: http://localhost:8090)
#   -h             Show this help
# =============================================================================

set -euo pipefail

# ── Defaults ──────────────────────────────────────────────────────────────────
DURATION=600
INTERVAL=6
API_URL="http://localhost:8090"

# ── Colours ───────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

# ── Argument parsing ──────────────────────────────────────────────────────────
while getopts "d:i:u:h" opt; do
  case $opt in
    d) DURATION=$OPTARG ;;
    i) INTERVAL=$OPTARG ;;
    u) API_URL=$OPTARG ;;
    h) sed -n '3,17p' "$0" | sed 's/^# \{0,2\}//'; exit 0 ;;
    *) echo "Unknown option: -$OPTARG"; exit 1 ;;
  esac
done

INGEST_URL="${API_URL}/api/tweets"
TOTAL=$(( DURATION / INTERVAL ))

# ── Tweet pool (varied sentiments and topics) ─────────────────────────────────
POSITIVE=(
  "Just got the new iPhone — the camera is absolutely incredible!"
  "Tesla's new update is a game changer for autopilot safety."
  "Google Cloud just saved us 3 hours of downtime. Impressive response."
  "Microsoft Copilot wrote my entire test suite in under two minutes."
  "Amazon same-day delivery arrived early. Very impressed."
  "The new MacBook performance blew me away on benchmarks."
  "Spotify's new AI playlist feature is genuinely great."
  "Netflix original series this week was outstanding."
  "OpenAI's latest model handles complex reasoning remarkably well."
  "Best customer support experience I have ever had. Five stars."
  "This product exceeded every single one of my expectations."
  "Incredible build quality — worth every penny spent."
  "The onboarding flow for this app is beautifully designed."
  "Finally a software update that actually fixes things!"
  "Our team productivity has doubled since switching tools."
)

NEGATIVE=(
  "Tesla autopilot nearly ran a red light again. Third time this month."
  "iPhone battery drains in four hours. Completely unacceptable."
  "Google Maps sent me the wrong way for the third time today."
  "Microsoft Teams crashed during my most important client call."
  "Amazon package arrived damaged and support was unhelpful."
  "This app crashes every single time I open it. One star."
  "Worst customer service experience I have ever had."
  "Paid for premium and the features still do not work properly."
  "The update broke everything that was working before."
  "Completely disappointed — returning this immediately."
  "Data was lost after the sync failure. Unforgivable bug."
  "Loading times are unbearable. The app is completely unusable."
  "I regret switching to this platform. Going back to the old one."
  "Hidden fees on the invoice. This is dishonest business practice."
  "Three weeks and the support ticket is still unresolved."
)

NEUTRAL=(
  "Just installed the latest update. Testing it out now."
  "Comparing the new model with the previous generation."
  "The delivery arrived at 3 PM as scheduled."
  "Reading the documentation before starting setup."
  "Attended the product webinar. Lots of information to process."
  "Switching from one plan to another this week."
  "The specs match what was listed on the website."
  "Migrating our database to the new server today."
  "Waiting for the next version before making a decision."
  "Running the benchmark tests on the new hardware."
  "The changelog lists about 40 fixes in this release."
  "Set up the integration between the two platforms."
  "Scheduled the review meeting for next Tuesday."
  "Downloaded the trial version to evaluate the features."
  "The API documentation has been updated again."
)

ALL_TWEETS=("${POSITIVE[@]}" "${NEGATIVE[@]}" "${NEUTRAL[@]}")
POOL_SIZE=${#ALL_TWEETS[@]}

# ── Helpers ───────────────────────────────────────────────────────────────────
check_deps() {
  for cmd in curl; do
    if ! command -v "$cmd" &>/dev/null; then
      echo -e "${RED}ERROR:${RESET} '$cmd' is required but not installed."; exit 1
    fi
  done
}

check_api() {
  if ! curl -sf "${API_URL}/api/health" -o /dev/null --max-time 3; then
    echo -e "${RED}ERROR:${RESET} Cannot reach ${API_URL}. Is the backend running?"
    exit 1
  fi
}

format_time() {
  local secs=$1
  printf "%02d:%02d" $(( secs / 60 )) $(( secs % 60 ))
}

# ── Main ──────────────────────────────────────────────────────────────────────
check_deps
check_api

echo ""
echo -e "${BOLD}${CYAN}══════════════════════════════════════════════${RESET}"
echo -e "${BOLD}  PulseStream — Continuous Tweet Feed${RESET}"
echo -e "${BOLD}${CYAN}══════════════════════════════════════════════${RESET}"
echo -e "  Target   : ${INGEST_URL}"
echo -e "  Duration : ${DURATION}s  ($(format_time $DURATION))"
echo -e "  Interval : ${INTERVAL}s  between tweets"
echo -e "  Expected : ~${TOTAL} tweets total"
echo -e "${BOLD}${CYAN}══════════════════════════════════════════════${RESET}"
echo -e "  Press ${BOLD}Ctrl+C${RESET} to stop early"
echo ""

SENT=0
FAILED=0
START=$SECONDS
END=$(( START + DURATION ))

# Trap Ctrl+C for a clean summary
trap 'echo ""; print_summary; exit 0' INT TERM

print_summary() {
  local elapsed=$(( SECONDS - START ))
  echo ""
  echo -e "${BOLD}${CYAN}── Final Summary ──────────────────────────────${RESET}"
  echo -e "  Elapsed : $(format_time $elapsed)"
  echo -e "  Sent OK : ${GREEN}${SENT}${RESET}"
  echo -e "  Failed  : $([ "$FAILED" -gt 0 ] && echo "${RED}${FAILED}${RESET}" || echo "${GREEN}0${RESET}")"
  echo -e "${BOLD}${CYAN}════════════════════════════════════════════════${RESET}"
  echo ""
}

IDX=0
while [[ $SECONDS -lt $END ]]; do
  ELAPSED=$(( SECONDS - START ))
  REMAINING=$(( END - SECONDS ))

  # Pick tweet (cycle through pool with slight randomisation)
  TWEET="${ALL_TWEETS[$((IDX % POOL_SIZE))]}"
  IDX=$(( IDX + 1 ))

  # Determine sentiment label for display
  if   [[ $IDX -le ${#POSITIVE[@]} ]] || \
       { [[ $IDX -gt $(( ${#POSITIVE[@]} + ${#NEGATIVE[@]} )) ]] && [[ $(( IDX % POOL_SIZE )) -lt ${#POSITIVE[@]} ]]; }; then
    LABEL="${GREEN}POS${RESET}"
  elif [[ $(( (IDX - 1) % POOL_SIZE )) -lt ${#POSITIVE[@]} ]]; then
    LABEL="${GREEN}POS${RESET}"
  elif [[ $(( (IDX - 1) % POOL_SIZE )) -lt $(( ${#POSITIVE[@]} + ${#NEGATIVE[@]} )) ]]; then
    LABEL="${RED}NEG${RESET}"
  else
    LABEL="${CYAN}NEU${RESET}"
  fi

  # Send the tweet
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "${INGEST_URL}" \
    -H "Content-Type: application/json" \
    -d "{\"text\": \"${TWEET}\", \"source\": \"continuous-test\"}" \
    --max-time 5 2>/dev/null || echo "000")

  if [[ "$HTTP_CODE" == "2"* ]]; then
    (( SENT++ )) || true
    STATUS="${GREEN}✓${RESET}"
  else
    (( FAILED++ )) || true
    STATUS="${RED}✗ (HTTP ${HTTP_CODE})${RESET}"
  fi

  # Progress line
  PROGRESS_PCT=$(( ELAPSED * 100 / DURATION ))
  BAR_FILL=$(( PROGRESS_PCT / 5 ))
  BAR=$(printf '#%.0s' $(seq 1 $BAR_FILL 2>/dev/null))
  printf "\r  [%-20s] %3d%%  sent:${GREEN}%d${RESET}  failed:${RED}%d${RESET}  remaining:%s  %b" \
    "$BAR" "$PROGRESS_PCT" "$SENT" "$FAILED" "$(format_time $REMAINING)" "$STATUS"

  sleep "$INTERVAL"
done

print_summary
