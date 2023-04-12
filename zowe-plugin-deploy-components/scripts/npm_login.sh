set -e
registry=$1
echo "//$(echo ${registry} | sed s/'http[s]\?:\/\/'//):_authToken=${NPM_TOKEN}" >> ~/.npmrc
echo "registry=${registry}" >> ~/.npmrc
npm whoami --registry=${registry}
