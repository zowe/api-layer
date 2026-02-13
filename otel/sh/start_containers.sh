#!/bin/sh

cd "$(dirname "$0")" || exit
docker compose stop

chmod -R 777 otel-*
docker compose up -d

echo "Checking OpenTelemetry Golden Tester..."
curl -s -v -w "\n" http://localhost:5318/v1/metrics -H "Content-Type: application/json" -d "{}" \
    --fail \
    --retry-all-errors \
    --retry-delay 10 \
    --retry 3
if [ "$?" -eq 0 ]; then
  echo "OpenTelemetry Golden Tester is ready!"
else
  echo "::error::OpenTelemetry Golden Tester startup failed"
  docker compose stop
  exit 1
fi

echo ""
echo "Checking OpenTelemetry Collector..."
curl -s -v -w "\n" /dev/null http://localhost:4318/v1/metrics -H "Content-Type: application/json" -d "{}" \
    --fail \
    --retry-all-errors \
    --retry-delay 10 \
    --retry 3
if [ $? -eq 0 ]; then
  echo "OpenTelemetry Collector is ready!"
else
  echo "::error::OpenTelemetry Collector startup failed"
  docker compose stop
  exit 1
fi
