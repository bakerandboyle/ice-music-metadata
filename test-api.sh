#!/usr/bin/env bash
# test-api.sh — Manual API verification for ICE Music Metadata Service
# Usage: ./test-api.sh [base_url]
#
# Re-runnable — generates unique data per run via timestamp suffix.
# Requires: curl, jq

set -uo pipefail

BASE_URL="${1:-http://localhost:8080}"
API="${BASE_URL}/api"
CT="Content-Type: application/json"
RUN_ID=$(date +%s)
ISRC_PREFIX=$(printf "GB%010d" "$RUN_ID" | tail -c 10)
PASS=0
FAIL=0

echo "Run ID: ${RUN_ID}"

# --- Helpers ---
header() { echo -e "\n\033[1;34m=== $1 ===\033[0m"; }
subtest() { echo -e "\n\033[1;33m--- $1 ---\033[0m"; }

check() {
    local desc="$1" expected="$2" actual="$3"
    if [ "$expected" = "$actual" ]; then
        echo -e "  \033[32m✓ ${desc}\033[0m (${actual})"
        ((PASS++))
    else
        echo -e "  \033[31m✗ ${desc}\033[0m (expected=${expected}, actual=${actual})"
        ((FAIL++))
    fi
}

show_request() {
    local method="$1" url="$2" body="${3:-}"
    echo -e "  \033[36m→ ${method} ${url}\033[0m"
    if [ -n "$body" ]; then
        echo -e "  \033[36m  Body: ${body}\033[0m"
    fi
}

show_response() {
    local code="$1" body="$2"
    echo -e "  \033[35m← ${code}\033[0m"
    echo "$body" | jq . 2>/dev/null || echo "  $body"
}

do_request() {
    local method="$1" url="$2" body="${3:-}" extra_headers="${4:-}"
    show_request "$method" "$url" "$body"

    local curl_args=(-s -w "\n%{http_code}" -X "$method" "$url" -H "${CT}")

    if [ -n "$extra_headers" ]; then
        while IFS= read -r h; do
            curl_args+=(-H "$h")
        done <<< "$extra_headers"
    fi

    if [ -n "$body" ]; then
        curl_args+=(-d "$body")
    fi

    LAST_RESP=$(curl "${curl_args[@]}")
    LAST_CODE=$(echo "$LAST_RESP" | tail -1)
    LAST_BODY=$(echo "$LAST_RESP" | sed '$d')
    show_response "$LAST_CODE" "$LAST_BODY"
}

isrc() {
    # Generate a unique 12-char ISRC: XX + 10-char suffix
    printf "%s%02d" "$ISRC_PREFIX" "$1"
}

# ============================================================
header "POST /api/artists — Create Artist"
# ============================================================

subtest "Happy path"
do_request POST "${API}/artists" "{\"name\": \"Test Artist ${RUN_ID}\"}" "X-Actor-Id: tester"
check "Status 201" "201" "$LAST_CODE"
TEST_ARTIST_ID=$(echo "$LAST_BODY" | jq -r '.id')
check "Has UUID id" "36" "${#TEST_ARTIST_ID}"
check "Name matches" "Test Artist ${RUN_ID}" "$(echo "$LAST_BODY" | jq -r '.name')"

subtest "Blank name — 400"
do_request POST "${API}/artists" '{"name": ""}'
check "Status 400" "400" "$LAST_CODE"

subtest "Missing name — 400"
do_request POST "${API}/artists" '{}'
check "Status 400" "400" "$LAST_CODE"

subtest "Idempotency — same key returns cached"
IDEM_KEY="idem-${RUN_ID}"
do_request POST "${API}/artists" "{\"name\": \"Idempotent ${RUN_ID}\"}" "X-Idempotency-Key: ${IDEM_KEY}"
ID1=$(echo "$LAST_BODY" | jq -r '.id')
do_request POST "${API}/artists" "{\"name\": \"Idempotent ${RUN_ID}\"}" "X-Idempotency-Key: ${IDEM_KEY}"
ID2=$(echo "$LAST_BODY" | jq -r '.id')
check "Same ID on retry" "$ID1" "$ID2"

# ============================================================
header "GET /api/artists/{id} — Fetch Artist"
# ============================================================

subtest "Happy path"
do_request GET "${API}/artists/${TEST_ARTIST_ID}"
check "Status 200" "200" "$LAST_CODE"
check "Name matches" "Test Artist ${RUN_ID}" "$(echo "$LAST_BODY" | jq -r '.name')"

subtest "Not found — 404 with Problem Details"
do_request GET "${API}/artists/00000000-0000-0000-0000-000000000000"
check "Status 404" "404" "$LAST_CODE"
check "RFC 7807 title" "Artist Not Found" "$(echo "$LAST_BODY" | jq -r '.title')"

# ============================================================
header "PATCH /api/artists/{id}/name — Edit Artist Name"
# ============================================================

subtest "Happy path"
RENAMED="Renamed ${RUN_ID}"
do_request PATCH "${API}/artists/${TEST_ARTIST_ID}/name" "{\"name\": \"${RENAMED}\"}" "X-Actor-Id: editor"
check "Status 200" "200" "$LAST_CODE"
check "Name updated" "${RENAMED}" "$(echo "$LAST_BODY" | jq -r '.name')"

subtest "Not found — 404"
do_request PATCH "${API}/artists/00000000-0000-0000-0000-000000000000/name" '{"name": "Nobody"}'
check "Status 404" "404" "$LAST_CODE"

subtest "Blank name — 400"
do_request PATCH "${API}/artists/${TEST_ARTIST_ID}/name" '{"name": "  "}'
check "Status 400" "400" "$LAST_CODE"

# ============================================================
header "GET /api/artists?name={name} — Search by Name"
# ============================================================

subtest "Search by canonical name"
ENCODED_NAME=$(echo -n "${RENAMED}" | jq -sRr @uri)
do_request GET "${API}/artists?name=${ENCODED_NAME}"
check "Status 200" "200" "$LAST_CODE"
check "Found 1 result" "1" "$(echo "$LAST_BODY" | jq 'length')"

subtest "Search with no match"
do_request GET "${API}/artists?name=NonexistentArtist${RUN_ID}"
check "Status 200" "200" "$LAST_CODE"
check "Empty array" "0" "$(echo "$LAST_BODY" | jq 'length')"

# ============================================================
header "POST /api/artists/{id}/aliases — Add Alias"
# ============================================================

ALIAS_NAME="Alias ${RUN_ID}"

subtest "Happy path"
do_request POST "${API}/artists/${TEST_ARTIST_ID}/aliases" "{\"alias\": \"${ALIAS_NAME}\"}" "X-Actor-Id: curator"
check "Status 201" "201" "$LAST_CODE"
check "Alias name" "${ALIAS_NAME}" "$(echo "$LAST_BODY" | jq -r '.aliasName')"

subtest "Search by alias resolves to artist"
ENCODED_ALIAS=$(echo -n "${ALIAS_NAME}" | jq -sRr @uri)
do_request GET "${API}/artists?name=${ENCODED_ALIAS}"
check "Found via alias" "${RENAMED}" "$(echo "$LAST_BODY" | jq -r '.[0].name')"

subtest "Not found artist — 404"
do_request POST "${API}/artists/00000000-0000-0000-0000-000000000000/aliases" '{"alias": "Ghost"}'
check "Status 404" "404" "$LAST_CODE"

subtest "Blank alias — 400"
do_request POST "${API}/artists/${TEST_ARTIST_ID}/aliases" '{"alias": "  "}'
check "Status 400" "400" "$LAST_CODE"

# ============================================================
header "POST /api/artists/{id}/tracks — Add Track"
# ============================================================

TRACK_ISRC=$(isrc 1)

subtest "Happy path"
do_request POST "${API}/artists/${TEST_ARTIST_ID}/tracks" \
    "{\"title\": \"Track ${RUN_ID}\", \"isrc\": \"${TRACK_ISRC}\", \"genre\": \"Pop\", \"durationSeconds\": 200}" \
    "X-Actor-Id: ingester"
check "Status 201" "201" "$LAST_CODE"
check "Title matches" "Track ${RUN_ID}" "$(echo "$LAST_BODY" | jq -r '.title')"
check "ISRC matches" "${TRACK_ISRC}" "$(echo "$LAST_BODY" | jq -r '.isrc')"

subtest "Duplicate ISRC — rejected"
do_request POST "${API}/artists/${TEST_ARTIST_ID}/tracks" \
    "{\"title\": \"Different Title\", \"isrc\": \"${TRACK_ISRC}\", \"genre\": \"Pop\", \"durationSeconds\": 180}"
check "Duplicate ISRC rejected" "true" "$([ "$LAST_CODE" -ge 400 ] && echo true || echo false)"

subtest "Missing title — 400"
do_request POST "${API}/artists/${TEST_ARTIST_ID}/tracks" \
    "{\"isrc\": \"$(isrc 2)\", \"genre\": \"Pop\", \"durationSeconds\": 200}"
check "Status 400" "400" "$LAST_CODE"

subtest "Invalid ISRC length — 400"
do_request POST "${API}/artists/${TEST_ARTIST_ID}/tracks" \
    '{"title": "Bad ISRC", "isrc": "SHORT", "genre": "Pop", "durationSeconds": 200}'
check "Status 400" "400" "$LAST_CODE"

subtest "Artist not found — 404"
do_request POST "${API}/artists/00000000-0000-0000-0000-000000000000/tracks" \
    "{\"title\": \"Orphan\", \"isrc\": \"$(isrc 3)\", \"genre\": \"Pop\", \"durationSeconds\": 200}"
check "Status 404" "404" "$LAST_CODE"

# ============================================================
header "GET /api/artists/{id}/tracks — Fetch Tracks"
# ============================================================

subtest "Happy path"
do_request GET "${API}/artists/${TEST_ARTIST_ID}/tracks"
check "Status 200" "200" "$LAST_CODE"
check "Has tracks" "true" "$([ "$(echo "$LAST_BODY" | jq 'length')" -gt 0 ] && echo true || echo false)"

subtest "Artist not found — 404"
do_request GET "${API}/artists/00000000-0000-0000-0000-000000000000/tracks"
check "Status 404" "404" "$LAST_CODE"

# ============================================================
header "GET /api/artist-of-the-day"
# ============================================================

subtest "Returns today's artist"
do_request GET "${API}/artist-of-the-day"
check "Status 200" "200" "$LAST_CODE"
AOD_NAME=$(echo "$LAST_BODY" | jq -r '.artist.name')
AOD_DATE=$(echo "$LAST_BODY" | jq -r '.date')
check "Has artist name" "true" "$([ "$AOD_NAME" != "null" ] && echo true || echo false)"
check "Has date" "true" "$([ "$AOD_DATE" != "null" ] && echo true || echo false)"
echo "  Today's artist: ${AOD_NAME} (${AOD_DATE})"

subtest "Deterministic — same result on retry"
do_request GET "${API}/artist-of-the-day"
check "Same artist on repeat" "$AOD_NAME" "$(echo "$LAST_BODY" | jq -r '.artist.name')"

# ============================================================
header "Actuator Endpoints"
# ============================================================

subtest "Health"
do_request GET "${BASE_URL}/actuator/health"
check "Status 200" "200" "$LAST_CODE"

subtest "Prometheus metrics"
show_request GET "${BASE_URL}/actuator/prometheus"
LAST_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/prometheus")
echo -e "  \033[35m← ${LAST_CODE}\033[0m (body omitted — large payload)"
check "Status 200" "200" "$LAST_CODE"

subtest "Info"
do_request GET "${BASE_URL}/actuator/info"
check "Status 200" "200" "$LAST_CODE"

# ============================================================
header "RESULTS"
# ============================================================

TOTAL=$((PASS + FAIL))
echo ""
echo "  Run ID: ${RUN_ID}"
echo -e "  \033[32mPassed: ${PASS}\033[0m"
echo -e "  \033[31mFailed: ${FAIL}\033[0m"
echo "  Total:  ${TOTAL}"
echo ""

exit "${FAIL}"
