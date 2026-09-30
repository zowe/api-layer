#!/bin/bash

# Verify a selection of log attributes exporter to a JSON file
# The goal of this verification is confirming the logs make it to the
# collector in a given configuration (ideally production-like)

# Log entries completeness is verified in OpenTelemetryResourceAttributesZosTest

set -e

while IFS= read -r line; do
    value1=$(echo "$line" | jq -r '(.resourceLogs.[0].resource.attributes.[] | select(.key == "deployment.environment.name").value.stringValue) // NOT_FOUND') # TEST
    value2=$(echo "$line" | jq -r '(.resourceLogs.[0].scopeLogs.[0].scope.name) // NOT_FOUND') # == org.zowe.apiml.opentelemetry
    value3=$(echo "$line" | jq -r '(.resourceLogs.[0].scopeLogs.[0].logRecords.[0].body.stringValue) // NOT_FOUND') # the log record
    value4=$(echo "$line" | jq -r '(.resourceLogs.[0].scopeLogs.[0].logRecords.[0].severityText) // NOT_FOUND') # INFO

    if [ "$value1" == "NOT_FOUND" ]; then
        echo ""
        exit 1
    fi
    if [ "$value2" == "NOT_FOUND" ]; then
        echo ""
        exit 1
    fi
    if [ "$value3" == "NOT_FOUND" ]; then
        echo ""
        exit 1
    fi
    if [ "$value4" == "NOT_FOUND" ]; then
        echo ""
        exit 1
    fi

done < ../logs.json
exit 0
