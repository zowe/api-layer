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
FROM python:3.11-slim

LABEL name="pythonsampleapp"
LABEL vendor="Zowe"
LABEL description="Python sample service for APIML onboarding"

WORKDIR /
#COPY onboarding-enabler-python ./onboarding-enabler-python

WORKDIR /usr/src/app
COPY Pipfile Pipfile.lock ./

RUN pip install --upgrade pip pipenv \
 && pipenv install --system --deploy

# ===== copy the app and config
COPY src ./src
COPY src/config/service-configuration.yml ./src/config/service-configuration.yml
COPY keystore ./keystore

# ===== expose service port
EXPOSE 10018

# ===== start
CMD ["python", "src/app.py"]
