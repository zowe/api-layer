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

INITIAL_ERRORS_FOUND=$ERRORS_FOUND

# - GATEWAY_PORT - should not be bound to a port currently
. ${ROOT_DIR}/bin/utils/network-utils.sh
validate_port_is_available ${GATEWAY_PORT}
validate_host_is_resolvable "ZOWE_EXPLORER_HOST" # Note - takes variable name, not value as parameter

# Validate some certificate values properly set. Needs more adding?
. ${ROOT_DIR}/bin/utils/zowe-variable-utils.sh
validate_zowe_prefix
validate_variable_is_set "KEYSTORE"
validate_variable_is_set "KEYSTORE_PASSWORD"
validate_variable_is_set "KEY_ALIAS"
validate_variable_is_set "VERIFY_CERTIFICATES"

. ${ROOT_DIR}/bin/utils/java-utils.sh
validate_java_home

return $ERRORS_FOUND-$INITIAL_ERRORS_FOUND