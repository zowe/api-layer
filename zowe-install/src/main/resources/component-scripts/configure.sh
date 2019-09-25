#!/bin/sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2019
################################################################################

. ${ROOT_DIR}/scripts/utils/configureJava.sh

# Add static definition for zosmf
cat <<EOF >$TEMP_DIR/zosmf.ebcidic.yml
# Static definition for z/OSMF
#
# Once configured you can access z/OSMF via the API gateway:
# http --verify=no GET https://$ZOWE_ZOSMF_HOST:$ZOWE_APIM_GATEWAY_PORT/api/v1/zosmf/info 'X-CSRF-ZOSMF-HEADER;'
#	
services:
    - serviceId: zosmf
      title: IBM z/OSMF
      description: IBM z/OS Management Facility REST API service
      catalogUiTileId: zosmf
      instanceBaseUrls:
        - https://$ZOWE_ZOSMF_HOST:$ZOWE_ZOSMF_PORT/zosmf/
      homePageRelativeUrl:  # Home page is at the same URL
      routedServices:
        - gatewayUrl: api/v1  # [api/ui/ws]/v{majorVersion}
          serviceRelativeUrl:
      apiInfo:
        - apiId: com.ibm.zosmf
          gatewayUrl: api/v1
          version: $ZOSMF_VERSION
          documentationUrl: https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.3.0/com.ibm.zos.v2r3.izua700/IZUHPINFO_RESTServices.htm
catalogUiTiles:
    zosmf:
        title: z/OSMF services
        description: IBM z/OS Management Facility REST services
EOF
iconv -f IBM-1047 -t IBM-850 ${STATIC_DEF_CONFIG_DIR}/zosmf.ebcidic.yml > $STATIC_DEF_CONFIG_DIR/zosmf.yml
rm ${STATIC_DEF_CONFIG_DIR}/zosmf.ebcidic.yml
chmod 770 $STATIC_DEF_CONFIG_DIR/zosmf.yml

#TODO - if zaf is running add the api-catalog iframe plugin