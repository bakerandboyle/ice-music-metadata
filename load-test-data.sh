#!/usr/bin/env bash
# load-test-data.sh — Seeds the ICE Music Metadata Service with sample data
# Usage: ./load-test-data.sh [base_url]
#
# Requires: curl, jq

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
API="${BASE_URL}/api"
ACTOR="data-loader"
CT="Content-Type: application/json"
ACTOR_H="X-Actor-Id: ${ACTOR}"

echo "=== Loading test data into ${API} ==="

# --- Helper ---
create_artist() {
    local name="$1"
    local id
    id=$(curl -s -X POST "${API}/artists" \
        -H "${CT}" -H "${ACTOR_H}" \
        -d "{\"name\": \"${name}\"}" | jq -r '.id')
    echo "${id}"
}

add_alias() {
    local artist_id="$1"
    local alias="$2"
    curl -s -X POST "${API}/artists/${artist_id}/aliases" \
        -H "${CT}" -H "${ACTOR_H}" \
        -d "{\"alias\": \"${alias}\"}" | jq .
}

add_track() {
    local artist_id="$1"
    local title="$2"
    local isrc="$3"
    local genre="$4"
    local duration="$5"
    curl -s -X POST "${API}/artists/${artist_id}/tracks" \
        -H "${CT}" -H "${ACTOR_H}" \
        -d "{\"title\": \"${title}\", \"isrc\": \"${isrc}\", \"genre\": \"${genre}\", \"durationSeconds\": ${duration}}" | jq .
}

# ============================================================
# ARTISTS
# ============================================================

echo ""
echo "--- Creating Artists ---"

QUEEN_ID=$(create_artist "Queen")
echo "Queen: ${QUEEN_ID}"

RADIOHEAD_ID=$(create_artist "Radiohead")
echo "Radiohead: ${RADIOHEAD_ID}"

BOWIE_ID=$(create_artist "David Bowie")
echo "David Bowie: ${BOWIE_ID}"

BJORK_ID=$(create_artist "Björk")
echo "Björk: ${BJORK_ID}"

MILES_ID=$(create_artist "Miles Davis")
echo "Miles Davis: ${MILES_ID}"

# ============================================================
# ALIASES
# ============================================================

echo ""
echo "--- Adding Aliases ---"

echo "Queen aliases:"
add_alias "${QUEEN_ID}" "The Queen Band"

echo "David Bowie aliases:"
add_alias "${BOWIE_ID}" "Ziggy Stardust"
add_alias "${BOWIE_ID}" "The Thin White Duke"
add_alias "${BOWIE_ID}" "David Robert Jones"

echo "Björk aliases:"
add_alias "${BJORK_ID}" "Björk Guðmundsdóttir"
add_alias "${BJORK_ID}" "The Sugarcubes"

echo "Miles Davis aliases:"
add_alias "${MILES_ID}" "The Prince of Darkness"

# ============================================================
# TRACKS
# ============================================================

echo ""
echo "--- Adding Tracks ---"

echo "Queen tracks:"
add_track "${QUEEN_ID}" "Bohemian Rhapsody"          "GBAYE7500101" "Rock"        354
add_track "${QUEEN_ID}" "Don't Stop Me Now"           "GBAYE7800201" "Rock"        209
add_track "${QUEEN_ID}" "Somebody to Love"            "GBAYE7600301" "Rock"        296
add_track "${QUEEN_ID}" "Under Pressure"              "GBAYE8100401" "Rock"        248
add_track "${QUEEN_ID}" "We Will Rock You"            "GBAYE7700501" "Rock"        122
add_track "${QUEEN_ID}" "We Are the Champions"        "GBAYE7700601" "Rock"        179

echo "Radiohead tracks:"
add_track "${RADIOHEAD_ID}" "Creep"                   "GBAYE9300101" "Alternative" 236
add_track "${RADIOHEAD_ID}" "Karma Police"            "GBAYE9700201" "Alternative" 264
add_track "${RADIOHEAD_ID}" "No Surprises"            "GBAYE9700301" "Alternative" 229
add_track "${RADIOHEAD_ID}" "Everything in Its Right Place" "GBAYE0000401" "Electronic" 250
add_track "${RADIOHEAD_ID}" "Idioteque"               "GBAYE0000501" "Electronic"  309

echo "David Bowie tracks:"
add_track "${BOWIE_ID}" "Space Oddity"                "GBAYE6900101" "Rock"        315
add_track "${BOWIE_ID}" "Heroes"                      "GBAYE7700701" "Art Rock"    371
add_track "${BOWIE_ID}" "Life on Mars?"               "GBAYE7100201" "Art Rock"    229
add_track "${BOWIE_ID}" "Starman"                     "GBAYE7200301" "Glam Rock"   257
add_track "${BOWIE_ID}" "Under Pressure"              "GBAYE8100402" "Rock"        248

echo "Björk tracks:"
add_track "${BJORK_ID}" "Hyperballad"                 "GBAYE9600101" "Electronic"  331
add_track "${BJORK_ID}" "Army of Me"                  "GBAYE9500201" "Electronic"  224
add_track "${BJORK_ID}" "Hunter"                      "GBAYE9700801" "Electronic"  248
add_track "${BJORK_ID}" "Jóga"                        "GBAYE9700901" "Electronic"  305

echo "Miles Davis tracks:"
add_track "${MILES_ID}" "So What"                     "USAYE5900101" "Jazz"        564
add_track "${MILES_ID}" "Blue in Green"               "USAYE5900201" "Jazz"        327
add_track "${MILES_ID}" "All Blues"                    "USAYE5900301" "Jazz"        696
add_track "${MILES_ID}" "Freddie Freeloader"          "USAYE5900401" "Jazz"        574

# ============================================================
# VERIFY
# ============================================================

echo ""
echo "=== Verification ==="

echo ""
echo "--- Artist of the Day ---"
curl -s "${API}/artist-of-the-day" | jq .

echo ""
echo "--- Lookup by alias: 'Ziggy Stardust' ---"
curl -s "${API}/artists?name=Ziggy%20Stardust" | jq .

echo ""
echo "--- Queen's tracks ---"
curl -s "${API}/artists/${QUEEN_ID}/tracks" | jq .

echo ""
echo "--- Artist count: $(curl -s "${API}/artists/${QUEEN_ID}" | jq -r '.name') and friends ---"
for id in "${QUEEN_ID}" "${RADIOHEAD_ID}" "${BOWIE_ID}" "${BJORK_ID}" "${MILES_ID}"; do
    curl -s "${API}/artists/${id}" | jq -r '.name'
done

echo ""
echo "=== Done: 5 artists, 7 aliases, 25 tracks loaded ==="
