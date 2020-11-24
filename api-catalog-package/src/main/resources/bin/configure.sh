#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2019, 2020
################################################################################

# Access API Catalog directly
CATALOG_GATEWAY_URL=https://${ZOWE_EXPLORER_HOST}:${GATEWAY_PORT}/ui/v1/apicatalog

# Create desktop app plugin
${ROOT_DIR}/bin/utils/zowe-install-iframe-plugin.sh \
  "org.zowe.api.catalog" \
  "API Catalog" \
  ${CATALOG_GATEWAY_URL} \
  "${WORKSPACE_DIR}/api-catalog" \
  "${ROOT_DIR}/components/api-catalog/assets/api-catalog.png"