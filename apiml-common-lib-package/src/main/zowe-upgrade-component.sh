#!/usr/bin/env sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2021
################################################################################

################################################################################
# Zowe updater script
#
# This script will upgrade a specified Zowe component to the latest version. It will
# get consumed by the zowe-install script during the Zowe installation.
################################################################################

#Downloading the Zowe component artifact from Zowe artifactory and saving it into the temporary components directory.

# Prepare shell environment
if [ -z "${ZOWE_ROOT_DIR}" ]; then
  export ZOWE_ROOT_DIR=$(cd $(dirname $0)/../../../;pwd)
fi

repository_path="libs-snapshot-local"

prepare_log_file() {
    if [ -z "${LOG_FILE}" ]; then
        set_install_log_directory "${LOG_DIRECTORY}"
        validate_log_file_not_in_root_dir "${LOG_DIRECTORY}" "${ZOWE_ROOT_DIR}"
        set_install_log_file "zowe-upgrade-component"
    else
        set_install_log_file_from_full_path "${LOG_FILE}"
        validate_log_file_not_in_root_dir "${LOG_FILE}" "${ZOWE_ROOT_DIR}"
    fi
}

error_handler(){
    print_error_message "$1"
    exit 1
}

download_apiml_artifacts() {
  artifact_group="apiml/sdk"
  path=https://zowe.jfrog.io/artifactory/$repository_path/org/zowe/$artifact_group/$artifact_name
  version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
  build=$(curl -s $path/"$version"/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
  full_name=$artifact_name-$build.zip
  print_and_log_message $path/"$version"/"$full_name"
  print_and_log_message "Downloading the ${artifact_name} artifact..."
  curl -s --output "${temporary_components_directory}" \
  $path/"$version"/"$full_name"
  rc=$?;

  if [ $rc != 0 ]; then
    error_handler "The ${artifact_name} artifact download failed."
  else
    print_and_log_message "The ${artifact_name} artifact has been downloaded."
  fi
}

download_other_artifacts() {
  repository_path=$1
  artifact_group=$2
  print_and_log_message $repository_path
  full_name=$3
  path=https://zowe.jfrog.io/artifactory/$repository_path/org/zowe/$artifact_group/[RELEASE]/$full_name
  print_and_log_message $path
  print_and_log_message "Downloading the ${artifact_name} artifact..."
  curl -s --output "${temporary_components_directory}" \
  $path
  rc=$?;

  if [ $rc != 0 ]; then
    error_handler "The ${artifact_name} artifact download failed."
  else
    print_and_log_message "The ${artifact_name} artifact has been downloaded into the directory ${temporary_components_directory}"
  fi
}

download_jobs_and_files_artifacts() {
  artifact_group=$1
  path=https://zowe.jfrog.io/artifactory/api/storage/libs-release-local/org/zowe/$artifact_group/?lastModified
  print_and_log_message $path
  url=$(curl -s "$path" | jq -r '.uri')
  url=$(curl -s "$url" | jq -r '.downloadUri')
  print_and_log_message $url
  print_and_log_message "Downloading the ${artifact_name} artifact..."
  curl -s --output "${temporary_components_directory}" \
  "$url"
  rc=$?;

  if [ $rc != 0 ]; then
    error_handler "The ${artifact_name} artifact download failed."
  else
    print_and_log_message "The ${artifact_name} artifact has been downloaded into the directory ${temporary_components_directory}"
  fi
}

#######################################################################
# Parse command line options
while [ $# -gt 0 ]; do #Checks for parameters
  arg="$1"
      case $arg in
          -o|--component-package)
              shift
              artifact_name=$(basename $1)
              temporary_components_directory=$(get_full_path "$1")
              print_and_log_message $1
              temporary_components_directory=$(cd "$temporary_components_directory" && cd .. && pwd)
              print_and_log_message "${temporary_components_directory}"
              shift
          ;;
          -l|--logs-dir) # Represents the path to the installation logs
              shift
              LOG_DIRECTORY=$1
              shift
          ;;
          *)
              error_handler "$1 is an invalid option\ntry: zowe-upgrade-component.sh -o <PATH_TO_COMPONENT>"
              shift
      esac
done

case $artifact_name in
  launcher)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "$repository_path" "launcher" "$full_name"
    ;;
  jobs-api-package)
    full_name=$artifact_name-[RELEASE].zip
    download_jobs_and_files_artifacts "explorer/jobs"
    ;;
  files-api-package)
    full_name=$artifact_name-[RELEASE].zip
    download_jobs_and_files_artifacts "explorer/files"
    ;;
  api-catalog-package | discovery-package | gateway-package | caching-service-package | apiml-common-lib-package)
    download_apiml_artifacts
    ;;
  explorer-ui-server)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "libs-release-local" "explorer-ui-server" "$full_name"
    ;;
  explorer-jes)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "libs-release-local" "explorer-jes" "$full_name"
    ;;
  explorer-mvs)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "libs-release-local" "explorer-mvs" "$full_name"
    ;;
  explorer-uss)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "libs-release-local" "explorer-uss" "$full_name"
    ;;
esac

prepare_log_file

exit 0
