#!/bin/bash

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright Contributors to the Zowe Project.
################################################################################

################################################################################
# prepare docker build context
#
# This script will be executed with 2 parameters:
# - linux-distro
# - cpu-arch

################################################################################
# This script prepares all required files we plan to put into zowe-launch-scripts
# image.
#
# Prereqs:
# - must run with Github Actions (with GITHUB_RUN_NUMBER and GITHUB_SHA)
# - must provide $GITHUB_PR_ID is it's pull request

# exit if there are errors
set -e

###############################
# check parameters
linux_distro=$1
cpu_arch=$2
if [ -z "${linux_distro}" ]; then
  echo "Error: linux-distro parameter is missing."
  exit 1
fi
if [ -z "${cpu_arch}" ]; then
  echo "Error: cpu-arch parameter is missing."
  exit 1
fi

################################################################################
# CONSTANTS
# this should be containers/zowe-launch-scripts
BASE_DIR=$(cd $(dirname $0);pwd)
REPO_ROOT_DIR=$(cd $(dirname $0)/../;pwd)
WORK_DIR=tmp

###############################
echo ">>>>> prepare basic files"
cd "${REPO_ROOT_DIR}"
package_version=$(cat gradle.properties | grep version= | awk -F= '{print $2}')
package_release=$(echo "${package_version}" | awk -F. '{print $1;}')
echo "    version: ${package_version}"

###############################
# copy Dockerfile
echo ">>>>> copy Dockerfile to ${linux_distro}/${cpu_arch}/Dockerfile"
cd "${BASE_DIR}"
mkdir -p "${linux_distro}/${cpu_arch}"
if [ ! -f Dockerfile ]; then
  echo "Error: Dockerfile file is missing."
  exit 2
fi
cat Dockerfile | sed -e "s#version=\"0\.0\.0\"#version=\"${package_version}\"#" -e "s#release=\"0\"#release=\"${package_release}\"#" > "${linux_distro}/${cpu_arch}/Dockerfile"

###############################
echo ">>>>> clean up folder"
rm -fr "${BASE_DIR}/${WORK_DIR}"
mkdir -p "${BASE_DIR}/${WORK_DIR}"

###############################
echo ">>>>> build package"
cd "${REPO_ROOT_DIR}"

./gradlew packageApiGateway
if [ ! -f "${REPO_ROOT_DIR}/gateway-package/build/distributions/gateway-package-null.zip" ]; then
  echo "Error: failed to build gateway-package-null.zip."
  exit 3
fi

./gradlew packageCommonLib
if [ ! -f "${REPO_ROOT_DIR}/apiml-common-lib-package/build/distributions/apiml-common-lib-package-null.zip" ]; then
  echo "Error: failed to build apiml-common-lib-package-null.zip."
  exit 3
fi
###############################
echo ">>>>> prepare basic files"
cd "${BASE_DIR}/${WORK_DIR}"
unzip "${REPO_ROOT_DIR}/gateway-package/build/distributions/gateway-package-null.zip"
chmod +x bin/*
mkdir "apiml-common-lib"
cd "apiml-common-lib"
unzip "${REPO_ROOT_DIR}/apiml-common-lib-package/build/distributions/apiml-common-lib-package-null.zip"

cd "${REPO_ROOT_DIR}"
cp README.md "${BASE_DIR}/${WORK_DIR}"
cp LICENSE "${BASE_DIR}/${WORK_DIR}"

###############################
# copy to target context
echo ">>>>> copy to target build context"
cp -r "${BASE_DIR}/${WORK_DIR}" "${BASE_DIR}/${linux_distro}/${cpu_arch}/component"

###############################
# done
echo ">>>>> all done"
