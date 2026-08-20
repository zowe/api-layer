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

validate_caching_storage_mode() {
  storage_mode="${ZWE_components_caching_service_storage_mode:-${ZWE_configs_storage_mode}}"
  storage_mode_lc=$(printf '%s' "${storage_mode}" | tr '[:upper:]' '[:lower:]')

  if [ -n "${storage_mode}" ] && [ "${storage_mode_lc}" != "infinispan" ]; then
    echo "API ML single service requires caching storage mode set to infinispan"
    return 1
  fi
}

print_formatted_debug "ZWELS" "apiml-service,bin/validate.sh:${LINENO}" "- Checking API ML single service caching storage mode is infinispan"
validate_this "validate_caching_storage_mode" "apiml-service,bin/validate.sh:${LINENO}"
