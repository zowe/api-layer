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

download_jobs_and_files_artifacts() {
  artifact_group=$1
  path=https://zowe.jfrog.io/artifactory/api/storage/libs-release-local/org/zowe/$artifact_group/?lastModified
  echo $path
  url=$(curl -s "$path" | jq -r '.uri')
  url=$(curl -s "$url" | jq -r '.downloadUri')
  echo $url
  echo "Downloading the ${artifact_name} artifact..."
  curl -s --output ./"${full_name}" \
  "$url"
  rc=$?;

  if [ $rc != 0 ]; then
    echo "The ${artifact_name} artifact download failed."
    exit 1
  else
    echo "The ${artifact_name} artifact has been downloaded."
  fi
}

unzip_artifacts() {
  #Unzipping the files and copying them into the known components directory
  echo "Copying the components into the target directory..."
  components_folder=../..
  unzip ./"${full_name}" -d "${components_folder}/${artifact_name}"
  rc=$?;
  rm ./"${full_name}"

  if [ $rc != 0 ]; then
    echo "Could not unzip the ${full_name} artifact."
    exit 1
  else
    echo "The ${full_name} artifact has been unzipped."
  fi
}

# TODO figure out why pax command fails
unpax_artifacts() {
  #Unpaxing the files and copying them into the known components directory
  echo "Copying the components into the target directory..."
  components_folder=../..
  echo $full_name
  pax -ppx -rf ./"${full_name}" "${components_folder}/${artifact_name}"
  rc=$?;
  rm ./"${full_name}"

  if [ $rc != 0 ]; then
    echo "Could not unpax the ${full_name} artifact."
    exit 1
  else
    echo "The ${full_name} artifact has been unpaxed."
  fi
}

case $artifact_name in
  launcher)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "$repository_path" "launcher" "$full_name"
    unpax_artifacts
    ;;
  jobs-api-package)
    full_name=$artifact_name-[RELEASE].zip
    download_jobs_and_files_artifacts "explorer/jobs"
    unzip_artifacts
    ;;
  files-api-package)
    full_name=$artifact_name-[RELEASE].zip
    download_jobs_and_files_artifacts "explorer/files"
    unzip_artifacts
    ;;
  api-catalog-package | discovery-package | gateway-package | caching-service-package | apiml-common-lib-package)
    download_apiml_artifacts
    unzip_artifacts
    ;;
  explorer-ui-server)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "libs-release-local" "explorer-ui-server" "$full_name"
    unpax_artifacts
    ;;
  explorer-jes)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "libs-release-local" "explorer-jes" "$full_name"
    unpax_artifacts
    ;;
  explorer-mvs)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "libs-release-local" "explorer-mvs" "$full_name"
    unpax_artifacts
    ;;
  explorer-uss)
    full_name=$artifact_name-[RELEASE].pax
    download_other_artifacts "libs-release-local" "explorer-uss" "$full_name"
    unpax_artifacts
    ;;
esac

# TODO leverage Zowe launcher to automatise the restart of Zowe. How we should integrate this script in zowe-install-packaging?
echo "The Zowe components have been updated, please restart Zowe."
exit 0
