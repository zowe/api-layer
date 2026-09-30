#!/bin/bash

# Verify a selection of log attributes exporter to a JSON file
# The goal of this verification is confirming the logs make it to the
# collector in a given configuration (ideally production-like)

# Log entries completeness is verified in OpenTelemetryResourceAttributesZosTest

set -e

if [ ! -f "otel-collector/logs.json" ] || [ ! -s "otel-collector/logs.json" ]; then
    echo "otel-collector/logs.json is not a regular file or it is empty"
    exit 1
fi

while IFS= read -r line; do
    value1=$(echo "$line" | jq -r '(.resourceLogs.[0].resource.attributes.[] | select(.key == "deployment.environment.name").value.stringValue) // "NOT_FOUND"') # TEST
    value2=$(echo "$line" | jq -r '(.resourceLogs.[0].scopeLogs.[0].scope.name) // "NOT_FOUND"') # == org.zowe.apiml.opentelemetry
    value3=$(echo "$line" | jq -r '(.resourceLogs.[0].scopeLogs.[0].logRecords.[0].body.stringValue) // "NOT_FOUND"') # the log record
    value4=$(echo "$line" | jq -r '(.resourceLogs.[0].scopeLogs.[0].logRecords.[0].severityText) // "NOT_FOUND"') # INFO

    if [ "$value1" == "NOT_FOUND" ]; then
        echo "deployment.environment.name not found in $line"
        exit 1
    elif [ "$value1" != "TEST" ]; then
        echo "$value1 does not match expected 'TEST' in $line"
        exit 1
    fi
    if [ "$value2" == "NOT_FOUND" ]; then
        echo ".resourceLogs.[0].scopeLogs.[0].scope.name not found in $line"
        exit 1
    elif [ "$value2" != "org.zowe.apiml.opentelemetry" ]; then
        echo "$value2 does not match expected 'org.zowe.apiml.opentelemetry' in $line"
        exit 1
    fi
    if [ "$value3" == "NOT_FOUND" ]; then
        echo "API ML log entry not found in $line"
        exit 1
    elif [ -z "$value3" ]; then
        echo "API ML log entry not found in $line"
        exit 1
    fi
    if [ "$value4" == "NOT_FOUND" ]; then
        echo "severity text not found in $line"
        exit 1
    elif [ "$value4" != "INFO" ]; then
        echo "$value4 does not match expected 'INFO' in $line"
        exit 1
    fi

done < otel-collector/logs.json
exit 0
