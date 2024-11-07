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

if [ -z "$ZWE_zowe_workspaceDirectory" ]; then
    echo "Workspace directory is not defined"
    exit 1
fi

echo "$(env)" > $ZWE_zowe_workspaceDirectory/api-mediation/api-defs/env

INSTANCE_ID=$ZWE_haInstance_id

if [ -z "$INSTANCE_ID" ]; then
    SOURCE_FILE=$ZWE_zowe_workspaceDirectory/api-mediation/api-defs/discovery.zosmf_static_definition_yaml_template.yml
else
    SOURCE_FILE=$ZWE_zowe_workspaceDirectory/api-mediation/api-defs/discovery.zosmf_static_definition_yaml_template.$INSTANCE_ID.yml
fi

DEST_FILE="$ZWE_zowe_workspaceDirectory/api-mediation/api-defs/zosmf.temp"

ZOSMF_SCHEME=https

if [ "$ZWE_zowe_network_client_tls_attls" = "true" ]; then
    ZOSMF_SCHEME=http
fi

if [ -e "$SOURCE_FILE" ]; then
    chtag -tc ISO8859-1 $SOURCE_FILE
    sed -e "s|%ZOSMF_SCHEME%|${ZOSMF_SCHEME}|g" $SOURCE_FILE > $DEST_FILE
    iconv -f IBM-1047 -t ISO8859-1 $DEST_FILE > $SOURCE_FILE
    chtag -tc ISO8859-1 $SOURCE_FILE
    rm -f $DEST_FILE
else
    echo "File $SOURCE_FILE not found"
fi
