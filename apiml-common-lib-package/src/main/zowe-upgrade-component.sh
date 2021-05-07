#!/usr/bin/env sh

################################################################################
# This program and the accompanying materials are made available under the terms of the
# Eclipse Public License v2.0 which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-v20.html
#
# SPDX-License-Identifier: EPL-2.0
#
# Copyright IBM Corporation 2019, 2020
################################################################################

################################################################################
# Zowe updater script
#
# This script will download all the Zowe components from the Artifactory and
# will be afterwards consumed by the zowe-install script.
################################################################################

#Downloading the Zowe components artifact from Zowe artifactory
artifact_name=$(basename $2)
repository_path="libs-snapshot-local"
temporary_components_directory=$PWD/$(cd ..)

download_apiml_artifacts() {
  artifact_group="apiml/sdk"
  path=https://zowe.jfrog.io/artifactory/$repository_path/org/zowe/$artifact_group/$artifact_name
  version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
  build=$(curl -s $path/"$version"/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
  full_name=$artifact_name-$build.zip
  echo $path/"$version"/"$full_name"
  echo "Downloading the ${artifact_name} artifact..."
  curl -s --output "${temporary_components_directory}" \
  $path/"$version"/"$full_name"
  rc=$?;

  if [ $rc != 0 ]; then
    echo "The ${artifact_name} artifact download failed."
    exit 1
  else
    echo "The ${artifact_name} artifact has been downloaded."
  fi
}

download_other_artifacts() {
  repository_path=$1
  artifact_group=$2
  echo $repository_path
  full_name=$3
  path=https://zowe.jfrog.io/artifactory/$repository_path/org/zowe/$artifact_group/[RELEASE]/$full_name
  echo $path
  echo "Downloading the ${artifact_name} artifact..."
  curl -s --output "${temporary_components_directory}" \
  $path
  rc=$?;

  if [ $rc != 0 ]; then
    echo "The ${artifact_name} artifact download failed."
    exit 1
  else
    echo "The ${artifact_name} artifact has been downloaded into the directory ${temporary_components_directory}"
  fi
}

download_jobs_and_files_artifacts() {
  artifact_group=$1
  path=https://zowe.jfrog.io/artifactory/api/storage/libs-release-local/org/zowe/$artifact_group/?lastModified
  echo $path
  url=$(curl -s "$path" | jq -r '.uri')
  url=$(curl -s "$url" | jq -r '.downloadUri')
  echo $url
  echo "Downloading the ${artifact_name} artifact..."
  curl -s --output "${temporary_components_directory}" \
  "$url"
  rc=$?;

  if [ $rc != 0 ]; then
    echo "The ${artifact_name} artifact download failed."
    exit 1
  else
    echo "The ${artifact_name} artifact has been downloaded into the directory ${temporary_components_directory}"
  fi
}

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

exit 0
