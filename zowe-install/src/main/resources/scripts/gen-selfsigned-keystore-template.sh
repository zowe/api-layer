mkdir -p keystore
mkdir -p temp

**JAVA_SETUP**
if [[ ":$PATH:" == *":$JAVA_HOME/bin:"* ]]; then
  echo "ZOWE_JAVA_HOME already exists on the PATH"
else
  echo "Appending ZOWE_JAVA_HOME/bin to the PATH..."
  export PATH=$PATH:$JAVA_HOME/bin
  echo "Done."
fi

export SERVER_ALIAS=api_gateway
export PWD=zoe_password
export SAN="DNS:**HOSTNAME**,DNS:localhost,DNS:apigateway,DNS:caapigateway,IP:**IPADDRESS**,IP:127.0.0.1"
export DNAME="CN=localhost_apim,OU=CA technologies,O=MFaaS,L=Prague,ST=Prague,C=CZ"

keytool -genkey -noprompt -alias $SERVER_ALIAS -keyalg RSA -keystore keystore/$SERVER_ALIAS.ks -dname "$DNAME" -ext SAN="$SAN" -storepass "$PWD" -keypass "$PWD"
keytool -export -noprompt -alias $SERVER_ALIAS -keystore keystore/$SERVER_ALIAS.ks -storepass "$PWD" -file temp/$SERVER_ALIAS'_cert'
keytool -import -noprompt -alias $SERVER_ALIAS -keystore keystore/$SERVER_ALIAS.ts -file temp/$SERVER_ALIAS'_cert' -storepass "$PWD"
rm -rf temp
