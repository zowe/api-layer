#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2021
################################################################################

# Test script for custom JVM parameters parsing in start.sh
# This script tests the ZWE_configs_jvm_* environment variable parsing logic
# by sourcing the actual parse_jvm_args.sh from the main resources

TESTS_PASSED=0
TESTS_FAILED=0

# Determine the path to the actual parse_jvm_args.sh
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PARSE_SCRIPT="${SCRIPT_DIR}/parse_jvm_args.sh"

# Verify the script exists
if [ ! -f "$PARSE_SCRIPT" ]; then
    echo "ERROR: Cannot find parse_jvm_args.sh at: $PARSE_SCRIPT"
    echo "Make sure you're running this test from the correct location."
    exit 1
fi

echo "Testing with: $PARSE_SCRIPT"
echo ""

# Colors for output (if terminal supports it)
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Function to run a test
run_test() {
    test_name="$1"
    expected="$2"
    actual="$3"
    
    # Trim leading/trailing whitespace for comparison
    expected_trimmed=$(echo "$expected" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    actual_trimmed=$(echo "$actual" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
    
    if [ "$expected_trimmed" = "$actual_trimmed" ]; then
        echo "${GREEN}PASS${NC}: $test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo "${RED}FAIL${NC}: $test_name"
        echo "  Expected: '$expected_trimmed'"
        echo "  Actual:   '$actual_trimmed'"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# Function to parse JVM params by sourcing the actual script
# We run in a subshell to isolate each test
# Redirect stdout to /dev/null to suppress the "Custom JVM options:" message
# and only capture the CUSTOM_JVM_OPTS variable value
parse_jvm_params() {
    (
        # Source the actual script - it sets CUSTOM_JVM_OPTS
        # Suppress the echo output from the script
        . "$PARSE_SCRIPT" >/dev/null 2>&1
        echo "$CUSTOM_JVM_OPTS"
    )
}

# Clear any existing ZWE_configs_jvm_ variables
clear_jvm_vars() {
    for var in $(env | grep "^ZWE_configs_jvm_" | cut -d= -f1); do
        unset "$var"
    done
}

clear_jvm_vars

echo "========================================"
echo "Testing JVM Parameter Parsing"
echo "========================================"
echo ""

# Test 1: Simple -X option with value
echo "Test 1: Simple -X option (Xss)"
clear_jvm_vars
export ZWE_configs_jvm_Xss="512k"
result=$(parse_jvm_params)
run_test "Xss=512k -> -Xss512k" "-Xss512k" "$result"

# Test 2: Another -X option
echo ""
echo "Test 2: Another -X option (Xmn)"
clear_jvm_vars
export ZWE_configs_jvm_Xmn="256m"
result=$(parse_jvm_params)
run_test "Xmn=256m -> -Xmn256m" "-Xmn256m" "$result"

# Test 3: -XX boolean option enabled (empty value)
echo ""
echo "Test 3: -XX boolean option enabled (empty value)"
clear_jvm_vars
export ZWE_configs_jvm_XX_UseG1GC=""
result=$(parse_jvm_params)
run_test "XX_UseG1GC='' -> -XX:+UseG1GC" "-XX:+UseG1GC" "$result"

# Test 4: -XX boolean option enabled (true)
echo ""
echo "Test 4: -XX boolean option enabled (true)"
clear_jvm_vars
export ZWE_configs_jvm_XX_UseG1GC="true"
result=$(parse_jvm_params)
run_test "XX_UseG1GC=true -> -XX:+UseG1GC" "-XX:+UseG1GC" "$result"

# Test 5: -XX boolean option disabled (false)
echo ""
echo "Test 5: -XX boolean option disabled (false)"
clear_jvm_vars
export ZWE_configs_jvm_XX_UseG1GC="false"
result=$(parse_jvm_params)
run_test "XX_UseG1GC=false -> -XX:-UseG1GC" "-XX:-UseG1GC" "$result"

# Test 6: -XX option with value
echo ""
echo "Test 6: -XX option with value"
clear_jvm_vars
export ZWE_configs_jvm_XX_MaxGCPauseMillis="200"
result=$(parse_jvm_params)
run_test "XX_MaxGCPauseMillis=200 -> -XX:MaxGCPauseMillis=200" "-XX:MaxGCPauseMillis=200" "$result"

# Test 7: -D system property with underscores converted to dots
echo ""
echo "Test 7: -D system property (underscores to dots)"
clear_jvm_vars
export ZWE_configs_jvm_Dmy_custom_property="myvalue"
result=$(parse_jvm_params)
run_test "Dmy_custom_property=myvalue -> -Dmy.custom.property=myvalue" "-Dmy.custom.property=myvalue" "$result"

# Test 8: -D system property without value
echo ""
echo "Test 8: -D system property without value"
clear_jvm_vars
export ZWE_configs_jvm_Denable_feature=""
result=$(parse_jvm_params)
run_test "Denable_feature='' -> -Denable.feature" "-Denable.feature" "$result"

# Test 9: -D system property with simple name (no underscores)
echo ""
echo "Test 9: -D system property with simple name"
clear_jvm_vars
export ZWE_configs_jvm_Ddebug="true"
result=$(parse_jvm_params)
run_test "Ddebug=true -> -Ddebug=true" "-Ddebug=true" "$result"

# Test 10: Multiple parameters at once
echo ""
echo "Test 10: Multiple parameters"
clear_jvm_vars
export ZWE_configs_jvm_Xss="512k"
export ZWE_configs_jvm_XX_UseG1GC="true"
export ZWE_configs_jvm_Dmy_prop="value"
result=$(parse_jvm_params)
# Note: order depends on env output order, so we check for presence of each
has_xss=$(echo "$result" | grep -c "\-Xss512k")
has_g1gc=$(echo "$result" | grep -c "\-XX:+UseG1GC")
has_prop=$(echo "$result" | grep -c "\-Dmy.prop=value")
if [ "$has_xss" -ge 1 ] && [ "$has_g1gc" -ge 1 ] && [ "$has_prop" -ge 1 ]; then
    echo "${GREEN}PASS${NC}: Multiple parameters all present"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo "${RED}FAIL${NC}: Multiple parameters"
    echo "  Result: '$result'"
    echo "  Expected to contain: -Xss512k, -XX:+UseG1GC, -Dmy.prop=value"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi

# Test 11: -X option without value (flag only)
echo ""
echo "Test 11: -X option without value (flag)"
clear_jvm_vars
export ZWE_configs_jvm_Xnoclassgc=""
result=$(parse_jvm_params)
run_test "Xnoclassgc='' -> -Xnoclassgc" "-Xnoclassgc" "$result"

# Test 12: -XX option with complex value
echo ""
echo "Test 12: -XX option with complex value"
clear_jvm_vars
export ZWE_configs_jvm_XX_HeapDumpPath="/tmp/dumps"
result=$(parse_jvm_params)
run_test "XX_HeapDumpPath=/tmp/dumps -> -XX:HeapDumpPath=/tmp/dumps" "-XX:HeapDumpPath=/tmp/dumps" "$result"

# Test 13: No JVM parameters set
echo ""
echo "Test 13: No JVM parameters set"
clear_jvm_vars
result=$(parse_jvm_params)
run_test "No params -> empty string" "" "$result"

# Test 14: -D property with nested dots simulation
echo ""
echo "Test 14: -D property with multiple underscores"
clear_jvm_vars
export ZWE_configs_jvm_Dspring_datasource_url="jdbc:h2:mem:test"
result=$(parse_jvm_params)
run_test "Dspring_datasource_url=... -> -Dspring.datasource.url=..." "-Dspring.datasource.url=jdbc:h2:mem:test" "$result"

# Test 15: -XX with numeric string that's not true/false
echo ""
echo "Test 15: -XX with numeric value"
clear_jvm_vars
export ZWE_configs_jvm_XX_ParallelGCThreads="4"
result=$(parse_jvm_params)
run_test "XX_ParallelGCThreads=4 -> -XX:ParallelGCThreads=4" "-XX:ParallelGCThreads=4" "$result"

# Clean up
clear_jvm_vars

echo ""
echo "========================================"
echo "Test Summary"
echo "========================================"
echo "Passed: $TESTS_PASSED"
echo "Failed: $TESTS_FAILED"
echo ""

if [ "$TESTS_FAILED" -gt 0 ]; then
    echo "${RED}Some tests failed!${NC}"
    exit 1
else
    echo "${GREEN}All tests passed!${NC}"
    exit 0
fi
