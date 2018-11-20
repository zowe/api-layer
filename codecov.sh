#!/bin/bash
# CODECOV_TOKEN needs to be set by caller
./gradlew build coverage
cd api-catalog-ui/frontend
npm run coverage
cd ../..
bash <(curl -s https://codecov.io/bash)
