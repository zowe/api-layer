## How to set ciphers used in API ML services

To set ciphers supported by the HTTPS servers in API ML services you need to set following property
for all services (gateway, discovery service, and API catalog):

    -Dapiml.security.ciphers=<cipher-list>

The default value is in the `application.yml` files, for example: [gateway-service/src/main/resources/application.yml](/gateway-service/src/main/resources/application.yml). The default configuration is in the resulting JAR files.

On z/OS, this can be overridden in `$ZOWE_ROOT_DIR/api-mediation/scripts/api-mediation-start-*.sh` where `*` expands to `gateway`, `catalog`, and `discovery`.

On localhost, the default configuration can be overridden on [config/local/gateway-service.yml](/config/local/gateway-service.yml) and other YAML files for development purposes without rebuilding the JAR files.

The default cipher list is:

    TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384

The IANA ciphers names are supported.
The names of ciphers are available at https://wiki.mozilla.org/Security/Server_Side_TLS#Cipher_suites.
