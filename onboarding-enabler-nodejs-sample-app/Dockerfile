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
ARG ZOWE_BASE_IMAGE=3-ubuntu

#FROM zowe-docker-release.jfrog.io/ompzowe/base-jdk:${ZOWE_BASE_IMAGE} AS builder
FROM node:20-alpine

LABEL name="hwexpress"
LABEL vendor="Zowe"
LABEL description="NodeJS sample service for APIML onboarding"

WORKDIR /
COPY onboarding-enabler-nodejs ./onboarding-enabler-nodejs

WORKDIR /app
COPY package*.json ./

RUN pwd
RUN ls -al
RUN npm config set strict-ssl false
RUN npm ci --legacy-peer-deps

# ===== copy the app and config
COPY src ./src
COPY config/service-configuration.yml ./config/service-configuration.yml

# ===== expose service port
EXPOSE 10020

# ===== start
CMD ["npm", "run", "start"]
