#!/usr/bin/env sh

name=gateway-package
path=https://zowe.jfrog.io/artifactory/libs-snapshot-local/org/zowe/apiml/sdk/gateway-package
version=$(curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/")
build=$(curl -s $path/$version/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/")
echo "$version"
zip=$name-$build.zip
echo "$zip"

curl --output ./gateway.zip \
$path/"$version"/"$zip"
#https://zowe.jfrog.io/artifactory/libs-snapshot-local/org/zowe/apiml/sdk/gateway-service-package/maven-metadata.xml
#https://zowe.jfrog.io/artifactory/libs-snapshot-local/org/zowe/apiml/sdk/gateway-package/1.21.12-SNAPSHOT/gateway-package-1.21.12-20210426.153328-4.zip
