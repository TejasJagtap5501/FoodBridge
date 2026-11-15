#!/usr/bin/env bash
# End-to-end smoke test against a running FoodBridge backend.
set -u
BASE="${BASE:-http://localhost:8080}"
pass=0; fail=0
ok()   { echo "  PASS  $1"; pass=$((pass+1)); }
bad()  { echo "  FAIL  $1"; fail=$((fail+1)); }
check(){ if [ "$2" = "$3" ]; then ok "$1 ($2)"; else bad "$1 (expected $3, got $2)"; fi; }

login() {
  curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])'
}

echo "== 1. Auth =="
ADMIN=$(login admin@foodbridge.dev 'Admin@12345')
DONOR=$(login donor@foodbridge.dev 'Donor@12345')
NGO=$(login ngo@foodbridge.dev 'Ngo@12345')
NGO2=$(login ngo2@foodbridge.dev 'Ngo2@12345')
PENDING=$(login ngo.pending@foodbridge.dev 'Ngo@12345')
[ -n "$ADMIN" ] && ok "admin login" || bad "admin login"
[ -n "$NGO" ] && ok "ngo login" || bad "ngo login"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' -d '{"email":"donor@foodbridge.dev","password":"WrongPass123"}')
check "wrong password rejected" "$code" "401"

code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/admin/stats" -H "Authorization: Bearer $DONOR")
check "donor blocked from admin API" "$code" "403"

code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/admin/stats")
check "anonymous blocked from admin API" "$code" "401"

echo "== 2. Password hashing =="
HASH=$(sudo mariadb -N -B -e "SELECT password_hash FROM foodbridge.users WHERE email='donor@foodbridge.dev'")
case "$HASH" in
  \$2a\$*|\$2b\$*|\$2y\$*) ok "password stored as BCrypt hash (${HASH:0:7}...)" ;;
  *) bad "password is not BCrypt: ${HASH:0:12}" ;;
esac

echo "== 3. Donation creation =="
EXP=$(python3 -c "import datetime;print((datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(hours=6)).strftime('%Y-%m-%dT%H:%M:%SZ'))")
PREP=$(python3 -c "import datetime;print((datetime.datetime.now(datetime.timezone.utc)-datetime.timedelta(hours=1)).strftime('%Y-%m-%dT%H:%M:%SZ'))")
BODY="{\"foodName\":\"Smoke Test Poha\",\"description\":\"Fresh poha\",\"quantity\":5,\"quantityUnit\":\"KG\",\"foodType\":\"VEGETARIAN\",\"categoryId\":1,\"preparedAt\":\"$PREP\",\"expiresAt\":\"$EXP\",\"pickupAddress\":\"Test Kitchen, Pune\",\"city\":\"Pune\",\"storageConditions\":\"Room temperature\",\"allergenInfo\":\"None\",\"safetyDeclaration\":true}"
NEW=$(curl -s -X POST "$BASE/api/donations" -H "Authorization: Bearer $DONOR" -H 'Content-Type: application/json' -d "$BODY")
DID=$(echo "$NEW" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
REF=$(echo "$NEW" | python3 -c 'import sys,json;print(json.load(sys.stdin)["referenceCode"])')
[ -n "$DID" ] && ok "donation created ($REF)" || bad "donation create: $NEW"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/donations" -H "Authorization: Bearer $NGO" -H 'Content-Type: application/json' -d "$BODY")
check "NGO cannot create donation" "$code" "403"

BAD='{"foodName":"","quantity":-1,"quantityUnit":"KG","foodType":"VEGETARIAN","categoryId":1,"preparedAt":"2020-01-01T00:00:00Z","expiresAt":"2020-01-02T00:00:00Z","pickupAddress":"x","city":"Pune","safetyDeclaration":false}'
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/donations" -H "Authorization: Bearer $DONOR" -H 'Content-Type: application/json' -d "$BAD")
check "invalid donation rejected" "$code" "400"

echo "== 4. NGO verification gate =="
STAMP=$(date +%s)
NEWEMAIL="smoke.ngo.$STAMP@example.org"
REGNO="MH/SMOKE/$STAMP"
REG=$(curl -s -X POST "$BASE/api/auth/register/ngo" -H 'Content-Type: application/json' -d "{
  \"email\":\"$NEWEMAIL\",\"password\":\"SmokeTest123\",\"fullName\":\"Smoke Tester\",
  \"organizationName\":\"Smoke Test Foundation\",\"registrationNumber\":\"$REGNO\",
  \"contactPerson\":\"Smoke Tester\",\"addressLine1\":\"1 Test Road\",\"city\":\"Pune\",
  \"state\":\"Maharashtra\",\"postalCode\":\"411001\"}")
NEWNGO=$(echo "$REG" | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/claims/donations/$DID" -H "Authorization: Bearer $NEWNGO" \
  -H 'Content-Type: application/json' -d '{"suitabilityCheck":true,"message":"hi"}')
check "newly registered (unverified) NGO cannot claim" "$code" "403"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/auth/register/ngo" -H 'Content-Type: application/json' -d "{
  \"email\":\"$NEWEMAIL\",\"password\":\"SmokeTest123\",\"fullName\":\"Smoke Tester\",
  \"organizationName\":\"Smoke Test Foundation\",\"registrationNumber\":\"$REGNO\",
  \"contactPerson\":\"Smoke Tester\",\"addressLine1\":\"1 Test Road\",\"city\":\"Pune\",
  \"state\":\"Maharashtra\",\"postalCode\":\"411001\"}")
check "duplicate email registration rejected" "$code" "409"

echo "== 5. Concurrent claim (two NGOs, same donation) =="
# Fire both claims as close to simultaneously as possible.
curl -s -X POST "$BASE/api/claims/donations/$DID" -H "Authorization: Bearer $NGO" \
  -H 'Content-Type: application/json' -d '{"suitabilityCheck":true,"message":"Can collect"}' \
  -o /tmp/c1.json -w '%{http_code}' > /tmp/c1.code &
P1=$!
curl -s -X POST "$BASE/api/claims/donations/$DID" -H "Authorization: Bearer $NGO2" \
  -H 'Content-Type: application/json' -d '{"suitabilityCheck":true,"message":"Can collect"}' \
  -o /tmp/c2.json -w '%{http_code}' > /tmp/c2.code &
P2=$!
wait $P1 $P2
CLAIM1=$(cat /tmp/c1.code); CLAIM2=$(cat /tmp/c2.code)
WINS=$(printf '%s\n%s\n' "$CLAIM1" "$CLAIM2" | grep -c '^201$')
check "exactly one of two simultaneous claims succeeds" "$WINS" "1"
S1=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/claims/donations/$DID" -H "Authorization: Bearer $NGO" -H 'Content-Type: application/json' -d '{"suitabilityCheck":true}')
echo "  (first parallel results: $CLAIM1 / $CLAIM2; repeat attempt: $S1)"
check "duplicate claim rejected" "$S1" "409"
ACTIVE=$(curl -s "$BASE/api/claims?status=ACTIVE&size=100" -H "Authorization: Bearer $ADMIN" | python3 -c "import sys,json;d=json.load(sys.stdin);print(sum(1 for c in d['content'] if c['donationId']==$DID))")
check "exactly one active claim on the donation" "$ACTIVE" "1"
CLAIMID=$(curl -s "$BASE/api/claims?status=ACTIVE&size=100" -H "Authorization: Bearer $ADMIN" | python3 -c "import sys,json;d=json.load(sys.stdin);print([c['id'] for c in d['content'] if c['donationId']==$DID][0])")
CLAIMER=$(curl -s "$BASE/api/claims?status=ACTIVE&size=100" -H "Authorization: Bearer $ADMIN" | python3 -c "import sys,json;d=json.load(sys.stdin);print([c['ngoId'] for c in d['content'] if c['donationId']==$DID][0])")
NGO_ID=$(curl -s "$BASE/api/me" -H "Authorization: Bearer $NGO" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
if [ "$CLAIMER" = "$NGO_ID" ]; then WINNER_TOKEN="$NGO"; LOSER_TOKEN="$NGO2"; else WINNER_TOKEN="$NGO2"; LOSER_TOKEN="$NGO"; fi
echo "  (claim $CLAIMID won by NGO id $CLAIMER)"

echo "== 6. Pickup workflow =="
SCHED=$(python3 -c "import datetime;print((datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(hours=2)).strftime('%Y-%m-%dT%H:%M:%SZ'))")
PICK=$(curl -s -X POST "$BASE/api/pickups/claims/$CLAIMID" -H "Authorization: Bearer $WINNER_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"scheduledAt\":\"$SCHED\",\"instructions\":\"Side entrance\"}")
PID=$(echo "$PICK" | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
[ -n "$PID" ] && ok "pickup scheduled" || bad "pickup schedule: $PICK"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/pickups/$PID/confirm-handover" -H "Authorization: Bearer $WINNER_TOKEN")
check "NGO cannot confirm donor handover" "$code" "403"

ST=$(curl -s -X POST "$BASE/api/pickups/$PID/confirm-handover" -H "Authorization: Bearer $DONOR" | python3 -c 'import sys,json;print(json.load(sys.stdin)["status"])')
check "donor handover confirmation" "$ST" "HANDED_OVER"

ST=$(curl -s -X POST "$BASE/api/pickups/$PID/confirm-collection" -H "Authorization: Bearer $WINNER_TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["status"])')
check "NGO collection completes pickup" "$ST" "COMPLETED"

DST=$(curl -s "$BASE/api/donations/$DID" -H "Authorization: Bearer $DONOR" | python3 -c 'import sys,json;print(json.load(sys.stdin)["status"])')
check "donation completed" "$DST" "COMPLETED"

echo "== 7. Ownership isolation =="
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/donations/$DID/cancel" -H "Authorization: Bearer $LOSER_TOKEN" -H 'Content-Type: application/json' -d '{"reason":"not mine"}')
if [ "$code" = "403" ] || [ "$code" = "409" ]; then ok "foreign donation modification blocked ($code)"; else bad "foreign edit returned $code"; fi

echo "== 8. Notifications & SSE auth =="
COUNT=$(curl -s "$BASE/api/notifications/unread-count" -H "Authorization: Bearer $DONOR" | python3 -c 'import sys,json;print(json.load(sys.stdin)["count"])')
[ "$COUNT" -ge 1 ] && ok "donor has $COUNT unread notifications" || bad "no notifications for donor"

code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/notifications/stream?access_token=garbage")
check "SSE rejects invalid token" "$code" "401"
NGO_CNT=$(curl -s "$BASE/api/notifications" -H "Authorization: Bearer $NGO" | python3 -c 'import sys,json;print(json.load(sys.stdin)["totalElements"])')
[ "$NGO_CNT" -ge 0 ] && ok "ngo notification history readable ($NGO_CNT)" || bad "ngo notifications"

echo "== 9. Admin platform stats =="
STATS=$(curl -s "$BASE/api/admin/stats" -H "Authorization: Bearer $ADMIN")
echo "$STATS" | python3 -c '
import sys,json
d=json.load(sys.stdin)
print("  users=%d donors=%d ngos=%d donations=%d completed=%d meals=%d"%(d["totalUsers"],d["totalDonors"],d["totalNgos"],d["totalDonations"],d["completedDonations"],d["estimatedMealsServed"]))
assert d["totalUsers"]>=7 and d["totalDonations"]>=20 and d["completedDonations"]>=16
print("  PASS  platform stats consistent")' || bad "platform stats"

echo "== 10. Pagination / sorting / filtering =="
P=$(curl -s "$BASE/api/donations/available?page=0&size=3&sortBy=expiresAt&direction=asc" -H "Authorization: Bearer $NGO" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["size"],len(d["content"]))')
check "browse pagination size" "$P" "3 3"
F=$(curl -s "$BASE/api/donations/available?categoryId=2&size=50" -H "Authorization: Bearer $NGO" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(all(c["categoryName"]=="Bakery & Bread" for c in d["content"]))')
check "category filter" "$F" "True"

echo "== 11. Admin NGO decision =="
NPID=$(curl -s "$BASE/api/admin/ngos?search=$REGNO" -H "Authorization: Bearer $ADMIN" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["content"][0]["profileId"])')
DEC=$(curl -s -X POST "$BASE/api/admin/ngos/$NPID/verification" -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"approved":true,"note":"Documents verified"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["verificationStatus"])')
check "admin approves NGO" "$DEC" "APPROVED"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/admin/ngos/$NPID/verification" -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"approved":true}')
check "double decision rejected" "$code" "409"

echo "== 12. Audit log =="
AUD=$(curl -s "$BASE/api/admin/audit-logs?size=5" -H "Authorization: Bearer $ADMIN" | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d["totalElements"],d["content"][0]["action"] if d["content"] else "none")')
echo "  audit entries=$AUD"
[ "${AUD%% *}" -ge 1 ] && ok "audit log recorded" || bad "audit log empty"

echo "== 13. Reports =="
REP=$(curl -s "$BASE/api/admin/reports?days=30" -H "Authorization: Bearer $ADMIN")
echo "$REP" | python3 -c 'import sys,json;d=json.load(sys.stdin);print("  series points=%d categories=%d cities=%d topDonors=%d"%(len(d["donationsPerDay"]),len(d["categoryBreakdown"]),len(d["topCities"]),len(d["topDonors"])))' || bad "reports"

echo "== 14. Health =="
HS=$(curl -s "$BASE/actuator/health" | python3 -c 'import sys,json;print(json.load(sys.stdin)["status"])')
check "actuator health" "$HS" "UP"

echo
echo "==================== $pass passed, $fail failed ===================="
[ "$fail" -eq 0 ] || exit 1
