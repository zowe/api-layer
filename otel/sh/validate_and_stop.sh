#!/bin/sh

# Assumes PWD in otel directory

set -e

echo "Waiting for Golden Validator to finish..."
# This blocks until the golden container exits (success or timeout)
EXIT_CODE_GOLDEN=$(docker wait golden)

echo "Stopping collector container..."
docker stop collector -t 60
echo "Collector container logs:"
> otel-collector/container.log
docker logs collector 2>&1 | tee otel-collector/container.log

# Display logs to see the diff if it failed
echo "Golden container logs:"
> otel-golden/container.log
docker logs golden 2>&1 | tee otel-golden/container.log

EXIT_CODE_LOGGERS=0
sh sh/validate_loggers.sh || EXIT_CODE_LOGGERS=$?

echo ""

if [ "$EXIT_CODE_GOLDEN" -ne 0 ]; then
  echo "::error::OpenTelemetry metrics data validation failed! See logs above for diff."
  exit 1
elif [ "$EXIT_CODE_LOGGERS" -ne 0 ]; then
  echo "::error::OpenTelemetry logs data verification failed! See logs above for diff."
  exit 2
fi

echo "OpenTelemetry data validation passed!"
