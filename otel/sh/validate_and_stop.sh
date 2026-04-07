#!/bin/sh

echo "Waiting for Golden Validator to finish..."
# This blocks until the golden container exits (success or timeout)
EXIT_CODE=$(docker wait golden)

echo "Stopping collector container..."
docker stop collector -t 60
echo "Collector container logs:"
docker logs collector 2>&1 | tee otel-collector/container.log

# Display logs to see the diff if it failed
echo "Golden container logs:"
docker logs golden 2>&1 | tee otel-golden/container.log

echo ""

if [ "$EXIT_CODE" -ne 0 ]; then
  echo "::error::OpenTelemetry data validation failed! See logs above for diff."
  exit 1
fi
echo "OpenTelemetry data validation passed!"
