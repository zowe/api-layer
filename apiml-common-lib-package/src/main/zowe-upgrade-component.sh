#!/usr/bin/env sh

#Downloading the Zowe components artifact from Zowe artifactory
artifact_name=$2
repository_path="libs-snapshot-local"

download_apiml_artifacts() {
  artifact_group="apiml/sdk"
  path=https://zowe.jfrog.io/artifactory/$repository_path/org/zowe/$artifact_group/$artifact_name
  version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
  build=$(curl -s $path/"$version"/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
  full_name=$artifact_name-$build.zip
  echo $path/"$version"/"$full_name"
  echo "Downloading the ${artifact_name} artifact..."
  curl -s --output ./"${full_name}" \
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
  curl -s --output ./"${full_name}" \
  $path
  rc=$?;

  if [ $rc != 0 ]; then
    echo "The ${artifact_name} artifact download failed."
    exit 1
  else
    echo "The ${artifact_name} artifact has been downloaded."
  fi
}

case $artifact_name in
  launcher)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "$repository_path" "launcher" "$full_name"
    ;;
  jobs-api-package)
    full_name=$artifact_name-[RELEASE].zip
    download_other_artifacts "libs-release-local" "explorer/jobs" "$full_name"
    ;;
  files-api-package)
    full_name=$artifact_name-[RELEASE].zip
    download_other_artifacts "libs-release-local" "explorer/files" "$full_name"
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

#Unzipping the files and copying them into the known components directory
echo "Copying the jars into the Zowe components directory..."

components_folder=../..
unzip ./"${full_name}" -d "${components_folder}/${artifact_name}"
rm ./"${full_name}"

# TODO leverage Zowe launcher to automatise the restart of Zowe
echo "The Zowe components have been updated, please restart Zowe."
exit 0
