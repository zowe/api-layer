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

FROM zowe-docker-release.jfrog.io/ompzowe/base-jdk:${ZOWE_BASE_IMAGE} AS builder
FROM node:20-alpine

LABEL name="hwexpress"
LABEL vendor="Zowe"
LABEL description="NodeJS sample service for APIML onboarding"

# ricreiamo il repo root
WORKDIR /repo

# copiamo i due progetti come fratelli
COPY onboarding-enabler-nodejs ./onboarding-enabler-nodejs
COPY onboarding-enabler-nodejs-sample-app ./onboarding-enabler-nodejs-sample-app

# installiamo dal sample app
WORKDIR /repo/onboarding-enabler-nodejs-sample-app
RUN npm ci --legacy-peer-deps

#WORKDIR /app
#
#COPY onboarding-enabler-nodejs ./onboarding-enabler-nodejs
## ===== install deps
#COPY onboarding-enabler-nodejs-sample-app/package*.json ./
#
## ===== copy app
#COPY onboarding-enabler-nodejs-sample-app .
#COPY config/service-configuration.yml config/service-configuration.yml

# ===== expose service port
EXPOSE 10020

# ===== start
CMD ["npm", "run", "start"]
