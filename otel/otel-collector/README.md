# Running OTel collector locally

Example command to run the collector locally using Dcoer

docker run -v $(pwd)/config/local/otel/config.yaml:/etc/otelcol/config.yaml -v $(pwd)/otel/otel-collector/export:/etc/otel-collector/export/ -p 4317:4317 -p 4318:4318 -p 55679:55679 otel/opentelemetry-collector:0.143.0 2>&1 | tee collector-output.txt # Optionally tee output for easier search later

This will mount a volume in the docker container to local ./otel/otel-collector/export

note that if the files in the export directory are deleted, the collector needs to be restarted
