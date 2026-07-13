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

print_formatted_debug "ZWELS" "apiml-service,bin/validate.sh:${LINENO}" "- Checking API ML single service caching storage mode is infinispan"
validate_this "(storage_mode=\"\${ZWE_components_caching_service_storage_mode:-\${ZWE_configs_storage_mode}}\"; [ -n \"\$storage_mode\" ] && [ \"\$(printf '%s' \"\$storage_mode\" | tr '[:upper:]' '[:lower:]')\" != \"infinispan\" ] && { echo \"API ML single service requires caching storage mode set to infinispan\"; exit 1; }; true; ) 2>&1" "apiml-service,bin/validate.sh:${LINENO}"
