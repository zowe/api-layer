# Run Client Certificate Authentication Sample

This project is a Java-based client that performs authentication using a client certificate. 
It utilizes the Apache HTTP Client to send an HTTPS request with client certificate authentication.

## Prerequisites

1. Java 17
2. A valid trusted client certificate stored in a PKCS12 keystore (.p12 or .pfx file)

## Running the application

1. Build the client-cert-auth-sample
2. Export the following environment variables:

   **macOS:**

   ```shell
   export API_URL="<API_URL>"
   export CLIENT_CERT_PATH="<CLIENT_CERT_PATH>"
   export CLIENT_CERT_PASSWORD="<CLIENT_CERT_PASSWORD>"
   export CLIENT_CERT_ALIAS="<CLIENT_CERT_ALIAS>"
   export PRIVATE_KEY_ALIAS="<PRIVATE_KEY_ALIAS>"
   ```
   **Windows:**

   ```shell
   set API_URL="<API_URL>"
   set CLIENT_CERT_PATH="<CLIENT_CERT_PATH>"
   set CLIENT_CERT_PASSWORD="<CLIENT_CERT_PASSWORD>"
   set CLIENT_CERT_ALIAS="<CLIENT_CERT_ALIAS>"
   set PRIVATE_KEY_ALIAS="<PRIVATE_KEY_ALIAS>"
   ```

3. Run the JAR located inside the `build/libs` folder with the SSL debug argument:
   ```shell
   java -jar client-cert-auth-sample.jar -Djavax.net.debug=all
   ```
This will output detailed information about the SSL handshake and certificate validation process.
