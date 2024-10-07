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
# This script provides utility functions for prepare.sh for zowe-launch-scripts images

# exit if there are errors
set -e

# CONSTANTS
# this should be containers/zowe-launch-scripts
BASE_DIR=$(cd $(dirname $0);pwd)
REPO_ROOT_DIR=$(cd $(dirname $0)/../../;pwd)
WORK_DIR=tmp

function checkParameters {
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
}

function prepareVersioning {
    echo ">>>>> prepare basic files"
    cd "${REPO_ROOT_DIR}"
    package_version=$(cat gradle.properties | grep version= | awk -F= '{print $2}')
    package_release=$(echo "${package_version}" | awk -F. '{print $1;}')
    echo "    version: ${package_version}"
}

function copyDockerfileToInternalStructure {
    linux_distro=$1
    cpu_arch=$2

    echo ">>>>> copy Dockerfile to ${linux_distro}/${cpu_arch}/Dockerfile"
    cd "${BASE_DIR}"
    mkdir -p "${linux_distro}/${cpu_arch}"
    if [ ! -f Dockerfile ]; then
      echo "Error: Dockerfile file is missing."
      exit 2
    fi
    cat Dockerfile | sed -e "s#version=\"0\.0\.0\"#version=\"${package_version}\"#" -e "s#release=\"0\"#release=\"${package_release}\"#" > "${linux_distro}/${cpu_arch}/Dockerfile"
}

function cleanUpWorkingFolder {
    echo ">>>>> clean up folder"
    rm -fr "${BASE_DIR}/${WORK_DIR}"
    mkdir -p "${BASE_DIR}/${WORK_DIR}"
}

function getIgnoredUiTasks {
    ui_project=$1
    echo "-x ${ui_project}:test -x ${ui_project}:npmLint"
}

function buildPackage {
    service_package=$1
    package_task=$2
    echo ">>>>> build package ${service_package}"
    cd $REPO_ROOT_DIR
    if [ -n "${GITHUB_PR_ID}" ]; then
      export BRANCH_NAME=PR-${GITHUB_PR_ID}
    else
      export BRANCH_NAME=${GITHUB_REF#refs/heads/}
    fi
    export BUILD_NUMBER=${GITHUB_RUN_NUMBER}
    ./gradlew $package_task
    if [ ! -f "${REPO_ROOT_DIR}/${service_package}/build/distributions/${service_package}.zip" ]; then
      echo "Error: failed to build ${service_package}.zip"
      exit 3
    fi
}

function buildApimlCommonPackage {
    buildPackage "apiml-common-lib-package" "packageCommonLib -x gateway-service:test -x discovery-service:test -x api-catalog-services:test -x api-catalog-ui:test -x api-catalog-ui:npmLint"
}

function preparePackage {
    service_package=$1
    subdirectory=$2

    echo ">>>>> prepare ${service_package}"
    mkdir -p "${BASE_DIR}/${WORK_DIR}/${subdirectory}"
    cd "${BASE_DIR}/${WORK_DIR}/${subdirectory}"
    unzip "${REPO_ROOT_DIR}/${service_package}/build/distributions/${service_package}.zip"
    chmod +x bin/*
}

function prepareBasicFiles {
    echo ">>>>> prepare basic files"
    cd "${REPO_ROOT_DIR}"
    cp README.md "${BASE_DIR}/${WORK_DIR}"
    cp LICENSE "${BASE_DIR}/${WORK_DIR}"
}

function copyToBuildContext {
    linux_distro=$1
    cpu_arch=$2
    echo ">>>>> copy to target build context"
    cp -r "${BASE_DIR}/${WORK_DIR}" "${BASE_DIR}/${linux_distro}/${cpu_arch}/component"
}
