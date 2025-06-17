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

print_formatted_debug "ZWELS" "apiml-service,bin/validate.sh:${LINENO}" "- Checking API ML Gateway Service port ${ZWE_components_gateway_port}"
validate_this "is_port_available \"${ZWE_components_gateway_port}\" 2>&1" "apiml-service,bin/validate.sh:${LINENO}"

print_formatted_debug "ZWELS" "apiml-service,bin/validate.sh:${LINENO}" "- Checking API ML Discovery Service port ${ZWE_components_discovery_port}"
validate_this "is_port_available \"${ZWE_components_discovery_port}\" 2>&1" "apiml-service,bin/validate.sh:${LINENO}"

