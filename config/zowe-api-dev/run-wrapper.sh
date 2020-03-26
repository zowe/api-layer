#!/bin/sh

export PATH=$PATH:$JAVA_HOME/bin

echo "*******************  ENVIRONMENT  *******************"
echo "Working directory "$dir
echo "JAVA_HOME "$JAVA_HOME
echo "PATH "$PATH
echo "basePort "$basePort
echo "******************* /ENVIRONMENT  *******************"


echo "Inflating dependencies"
cd $dir/bin/lib
jar -xvf libraries.zip
