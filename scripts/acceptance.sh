#!/usr/bin/env bash
#
# Execute basic checks on the application
#
# If the test breaks unexpectedly, you will find the the server log
# and the temp files used in the assertion in the directory "$tmp_dir"
#

build_command="./gradlew clean build"
run_command="SERVER_PORT=57134 java -jar build/libs/developer-joyofenergy-java.jar"
base_url="http://localhost:57134"
tmp_dir="/tmp/joe"

# No customization needed beyond this point

# ----------------------------
# function definitions
# ----------------------------

fail() {
  echo $*
  exit 1
}

set -o pipefail
assert_json() {
  local expected=$1
  local command=$2
  echo "$expected"   | jq > "$tmp_dir/expected.json"
  bash -c "$command" | jq > "$tmp_dir/actual.json" || {
    exit $?
  }
  diff "$tmp_dir/expected.json" "$tmp_dir/actual.json" || {
    fail "Differences found in $command"
  }
}

server_is_up() {
  curl --fail --silent $base_url/readings/read/smart-meter-0 > /dev/null
}

# Common options we use with curl
curl="curl --fail --silent --show-error"

# ----------------------------
# setup
# ----------------------------

# use this argument to avoid rebuilding the jar when testing this script
if [ "$1" == "--skip-build" ]; then
  skip_build="true"
fi

# ensure the tools we rely on are installed
jq --version > /dev/null 2>&1 || fail 'please install "jq"'
curl --version > /dev/null 2>&1 || fail 'please install "curl"'

# ensure we execute in the root dir of the project
cd "$(dirname "$0")/.." || exit 1

# create temporary dir
mkdir -p "$tmp_dir" || exit 1

# ensure we are not talking to a previously run server
if server_is_up; then
  fail "Server already running"
fi

# build and run the server
if [ "$skip_build" != "true" ]; then
  sh -c "$build_command" || fail "build failed"
fi
sh -c "$run_command" > "$tmp_dir/server.log" &
run_pid=$!

# kill the server when the script exits
trap "kill $run_pid" exit

# wait for the server to be up
max_wait=10
for (( i = 1; i <= max_wait; i++ )); do
  if server_is_up; then
    echo server is up
    break
  fi
  echo "wait for server to come up"
  sleep 1
done
if [ $i -eq $max_wait ]; then
  fail "server did not come up"
fi

# ----------------------------
# test storing readings
# ----------------------------

TEST_METER=test-meter-0

payload='
{
  "smartMeterId": "'$TEST_METER'",
  "electricityReadings": [
    {
      "time": "2024-01-01T07:01:00.000000Z",
      "reading": 0.1
    }
    ,{
      "time": "2024-01-01T07:02:00.000000Z",
      "reading": 0.2
    }
    ,{
      "time": "2024-01-01T07:03:00.000000Z",
      "reading": 0.3
    }
    ,{
      "time": "2024-01-01T07:04:00.000000Z",
      "reading": 0.4
    }
  ]
}
'

$curl -d "$payload" -H "Content-Type: application/json" $base_url/readings/store || {
  fail "could not store readings"
}

echo "OK storing data"

# ----------------------------
# test reading from smart meter
# ----------------------------

expected='
[
  {
    "time": "2024-01-01T07:01:00Z",
    "reading": 0.1
  },
  {
    "time": "2024-01-01T07:02:00Z",
    "reading": 0.2
  },
  {
    "time": "2024-01-01T07:03:00Z",
    "reading": 0.3
  },
  {
    "time": "2024-01-01T07:04:00Z",
    "reading": 0.4
  }
]'
assert_json "$expected" "$curl $base_url/readings/read/$TEST_METER"
echo "OK reading data"

# ----------------------------
# test comparing all price plans
# ----------------------------

expected='
{
  "pricePlanComparisons": {
    "price-plan-2": 6.0,
    "price-plan-1": 12.0,
    "price-plan-0": 60.0,
    "price-plan-3": 300.0
  },
  "pricePlanId": "price-plan-0"
}'

assert_json "$expected" "$curl $base_url/price-plans/compare-all/$TEST_METER"
echo "OK comparing all price plans"

# ----------------------------
# test recommended price plan
# ----------------------------

expected='[
  {
    "price-plan-2": 6.0
  },
  {
    "price-plan-1": 12.0
  }
]'

assert_json "$expected" "$curl $base_url/price-plans/recommend/$TEST_METER?limit=2"
echo "OK recommended price plan"

# ----------------------------
# test assigning price plan to meter
# ----------------------------

# Use an existing meter from the default configuration instead of TEST_METER
EXISTING_METER="smart-meter-0"  # This meter exists in the default AccountService configuration

# Test successful assignment
assign_payload='
{
  "smartMeterId": "'$EXISTING_METER'",
  "pricePlanId": "price-plan-1"
}
'

$curl -d "$assign_payload" -H "Content-Type: application/json" $base_url/price-plans/assign-meter || {
  fail "could not assign price plan to meter"
}
echo "OK assigning price plan to existing meter"

# Test assignment with different price plan
assign_payload_2='
{
  "smartMeterId": "'$EXISTING_METER'",
  "pricePlanId": "price-plan-2"
}
'

$curl -d "$assign_payload_2" -H "Content-Type: application/json" $base_url/price-plans/assign-meter || {
  fail "could not reassign price plan to meter"
}
echo "OK reassigning price plan to existing meter"

# Test validation error for empty smart meter ID
invalid_assign_payload='
{
  "smartMeterId": "",
  "pricePlanId": "price-plan-1"
}
'

# Use curl without --fail for error cases to check status code
response_code=$(curl --silent --show-error --write-out "%{http_code}" --output /dev/null \
  -d "$invalid_assign_payload" -H "Content-Type: application/json" $base_url/price-plans/assign-meter)

if [ "$response_code" != "400" ]; then
  fail "expected 400 but got $response_code for empty smart meter ID"
fi
echo "OK validation error for empty smart meter ID"

# Test validation error for empty price plan ID
invalid_assign_payload_2='
{
  "smartMeterId": "'$EXISTING_METER'",
  "pricePlanId": ""
}
'

response_code=$(curl --silent --show-error --write-out "%{http_code}" --output /dev/null \
  -d "$invalid_assign_payload_2" -H "Content-Type: application/json" $base_url/price-plans/assign-meter)

if [ "$response_code" != "400" ]; then
  fail "expected 400 but got $response_code for empty price plan ID"
fi
echo "OK validation error for empty price plan ID"

# Test error for non-existent meter
non_existent_meter_payload='
{
  "smartMeterId": "non-existent-meter",
  "pricePlanId": "price-plan-1"
}
'

response_code=$(curl --silent --show-error --write-out "%{http_code}" --output /dev/null \
  -d "$non_existent_meter_payload" -H "Content-Type: application/json" $base_url/price-plans/assign-meter)

if [ "$response_code" != "400" ]; then
  fail "expected 400 but got $response_code for non-existent meter"
fi
echo "OK error for non-existent meter"

# Test error for non-existent price plan
non_existent_plan_payload='
{
  "smartMeterId": "'$EXISTING_METER'",
  "pricePlanId": "non-existent-price-plan"
}
'

response_code=$(curl --silent --show-error --write-out "%{http_code}" --output /dev/null \
  -d "$non_existent_plan_payload" -H "Content-Type: application/json" $base_url/price-plans/assign-meter)

if [ "$response_code" != "400" ]; then
  fail "expected 400 but got $response_code for non-existent price plan"
fi
echo "OK error for non-existent price plan"

# Test malformed JSON
response_code=$(curl --silent --show-error --write-out "%{http_code}" --output /dev/null \
  -d '{"invalid": json}' -H "Content-Type: application/json" $base_url/price-plans/assign-meter)

if [ "$response_code" != "400" ] && [ "$response_code" != "500" ]; then
  fail "expected 400 or 500 but got $response_code for malformed JSON"
fi
echo "OK error for malformed JSON"

# ----------------------------
# cleanup
# ----------------------------

rm -rf "$tmp_dir"
echo "OK all good!"