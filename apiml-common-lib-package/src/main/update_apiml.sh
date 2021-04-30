#!/usr/bin/env sh

#Downloading the Gateway Service artifact from Zowe artifactory
artifact_name=gateway-package
path=https://zowe.jfrog.io/artifactory/libs-snapshot-local/org/zowe/apiml/sdk/$artifact_name
version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
build=$(curl -s $path/"$version"/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
zip=$artifact_name-$build.zip

echo "Downloading the Gateway artifact..."
curl -s --output ./gateway.zip \
$path/"$version"/"$zip"
rc=$?;

if [ $rc != 0 ]; then
  echo "The Gateway artifact download failed."
  exit 1
else
  echo "The Gateway artifact has been downloaded."
fi

#Downloading the API Catalog Service artifact from Zowe artifactory
artifact_name=api-catalog-package
path=https://zowe.jfrog.io/artifactory/libs-snapshot-local/org/zowe/apiml/sdk/$artifact_name
version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
build=$(curl -s $path/"$version"/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
zip=$artifact_name-$build.zip

echo "Downloading the API Catalog Service artifact..."
curl -s --output ./api-catalog.zip \
$path/"$version"/"$zip"
rc=$?;

if [ $rc != 0 ]; then
  echo "The API Catalog Service artifact download failed."
  exit 1
else
  echo "The API Catalog Service artifact has been downloaded."
fi

#Downloading the Discovery Service artifact from Zowe artifactory
artifact_name=discovery-package
path=https://zowe.jfrog.io/artifactory/libs-snapshot-local/org/zowe/apiml/sdk/$artifact_name
version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
build=$(curl -s $path/"$version"/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
zip=$artifact_name-$build.zip

echo "Downloading the Discovery Service artifact..."
curl -s --output ./discovery.zip \
$path/"$version"/"$zip"
rc=$?;

if [ $rc != 0 ]; then
  echo "The Discovery Service artifact download failed."
  exit 1
else
  echo "The Discovery Service artifact has been downloaded."
fi

#Downloading the Caching Service artifact from Zowe artifactory
artifact_name=caching-service-package
path=https://zowe.jfrog.io/artifactory/libs-snapshot-local/org/zowe/apiml/sdk/$artifact_name
version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
build=$(curl -s $path/"$version"/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
zip=$artifact_name-$build.zip

echo "Downloading the Caching Service artifact..."
curl -s --output ./caching.zip \
$path/"$version"/"$zip"
rc=$?;

if [ $rc != 0 ]; then
  echo "The Caching Service artifact download failed."
  exit 1
else
  echo "The Caching Service artifact has been downloaded."
fi

#Unzipping the files and copying them into the known components directory
echo "Copying the jars into the Zowe components directory..."

components_folder=../..
unzip ./gateway.zip -d "${components_folder}/gateway"
rm ./gateway.zip

unzip ./discovery.zip -d "${components_folder}/discovery"
rm ./discovery.zip

unzip ./api-catalog.zip -d "${components_folder}/api-catalog"
rm ./api-catalog.zip

unzip ./caching.zip -d "${components_folder}/caching-service"
rm ./caching.zip

echo "The API Mediation Layer has been updated, please restart Zowe."
exit 0
