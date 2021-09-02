#######################################################################
# This program and the accompanying materials are made available
# under the terms of the Eclipse Public License v2.0 which
# accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright Contributors to the Zowe Project.
#######################################################################

# base image tag
ARG ZOWE_BASE_IMAGE=latest-ubuntu

FROM zowe-docker-release.jfrog.io/ompzowe/base-jdk:${ZOWE_BASE_IMAGE} AS builder

##################################
# labels
LABEL name="API Gateway" \
      maintainer="carson.cook@ibm.com" \
      vendor="Zowe" \
      version="0.0.0" \
      release="0" \
      summary="API Mediation Layer Gateway" \
      description="API Gateway service to route requests to services registered in the API Mediation Layer and provides an API for mainframe security."

##################################
# switch context
USER zowe
WORKDIR /component

##################################
# copy files
COPY --chown=zowe:zowe component .
COPY --chown=zowe:zowe component/LICENSE /licenses

##################################
# start command
EXPOSE 7554
ENTRYPOINT [ "bin/start.sh" ]
