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

# Load utility functions
. ../prepare-utils.sh

linux_distro=$1
cpu_arch=$2
checkParameters $linux_distro $cpu_arch

prepareVersioning
copyDockerfileToInternalStructure $linux_distro $cpu_arch
cleanUpWorkingFolder

zaas_package="zaas-package"
apiml_common_package="apiml-common-lib-package"

buildPackage $zaas_package "packageZaas"
buildApimlCommonPackage

preparePackage $zaas_package
preparePackage $apiml_common_package "apiml-common-lib"
prepareBasicFiles

copyToBuildContext $linux_distro $cpu_arch

echo ">>>>> all done"
