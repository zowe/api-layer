# Zowe API Mediation Layer Security Overview

Review this article to learn about topics which address security features in Zowe API Mediation Layer.

  - [How API ML transport security works](#how-api-ml-transport-security-works)
    - [Transport layer security](#transport-layer-security)
    - [Authentication](#authentication)
    - [Zowe API ML services](#zowe-api-ml-services)
    - [Zowe API ML TLS requirements](#zowe-api-ml-tls-requirements)
  - [Setting ciphers for API ML services](#setting-ciphers-for-api-ml-services)
  - [JSON Web Token (JWT)](#json-web-tokenjwt)
  - [z/OSMF JSON Web Tokens Support](#zosmf-json-web-tokens-support)

## How API ML transport security works

Security within the API Mediation Layer (API ML) is performed on several levels. This article describes how API ML uses Transport Layer Security (TLS). As a system administrator or API developer, use this guide to familiarize yourself with the following security concepts:

### Transport layer security

The TLS protocol should be used to ensure secure data-transport for all connections to API Mediation Layer services. While it is possible to disable the TLS protocol for debugging purposes or other use-cases, the enabled TLS protocol is the default mode.

### Authentication

Authentication is how an entity, whether it be a user (API Client), or an application (API Service), proves its true identity.

API ML uses the following authentication methods:

- **User ID and password**
    - The user ID and password are used to retrieve authentication tokens
    - Requests originate from a user
    - The user ID and password are validated by a z/OS security manager whereby
    a token is issued that is then used to access the API service

- **TLS client certificates**
    - Certificates are used for service-only requests

- **OIDC authentication**
 
  API ML is now able to authenticate mainframe users with an external/distributed Identity Provider (IDP) implemented by an OIDC/OAuth2 provider, such as OKTA, KeyCloak, Microsoft Entra ID (a.k.a. Azure Active Directory) and others.
  Client applications can ask their users to log in at the authentication page of the OIDC provider, and then access APIs with the JWT Access Token that is provided by the external IDP. The following process outlines that basic flow:
  - The client application intiates the OIDC authentication flow with the distributed OIDC provider.  
  - The user provides credentials as required at the provider's authentication page/end-point.
  - The client application obtains authorization in the form of code that is exchanged for access JWT token (or an Identity JWT and Refresh JWT).
  - The client application passes the access JWT token to the API ML Gateway with subsequent requests for mainframe resources.
  - API ML federates the user identities and calls the requested resource with appropriate mainframe user credentials.

For more information about OIDC authentication and Identity Federation, see [Authenticating with OIDC](https://docs.zowe.org/stable/extend/extend-apiml/api-mediation-oidc-authentication/).

### Zowe API ML services

The following range of service types apply to the Zowe&trade; API ML:

- **Zowe API ML services**

  - **Gateway Service (GW)**  
    The Gateway is the access point for API clients that require access to API services.
    API services can be accessed through the Gateway by API Clients. The Gateway receives information about an API Service
    from the Discovery Service.

  - **Discovery Service (DS)**  
    The Discovery Service collects information about API services and provides this information to the Gateway
    and other services. API ML internal services also register to the Discovery Service.

  - **API Catalog (AC)**  
    The Catalog displays information about API services through a web UI. The Catalog receives information
    about an API service from the Discovery Service.

- **Authentication and Authorization Service (AAS)**  
    AAS provides authentication and authorization functionality to check user access to resources on z/OS.
    The API ML uses z/OSMF API for  authentication. 

- **API Clients**  
    API Clients are external applications, users, or other API services that access API services through the API Gateway

- **API Services**  
    API services are applications that are accessed through the API Gateway. API services register themselves to the
    Discovery Service and can access other services through the Gateway. If an API service is installed
    so that direct access is possible, API services can access other services without the Gateway.
    When APIs access other services, they can also function as API clients.

### Zowe API ML TLS requirements

The API ML TLS requires servers to provide HTTPS ports. Each API ML service has the following specific requirements:

- **API Client**
    - The API Client is not a server
    - Requires trust of the API Gateway
    - Has a truststore or SAF keyring that contains certificates required to trust the Gateway

- **Gateway Service**
    - Provides an HTTPS port
    - Has a keystore or SAF keyring with a server certificate
        - The certificate needs to be trusted by API Clients
        - This certificate should be trusted by web browsers because the API Gateway can be used to display web UIs
    - Has a truststore or SAF keyring that contains certificates needed to trust API Services

- **API Catalog**
    - Provides an HTTPS port
    - Has a keystore or SAF keyring with a server certificate
        - The certificate needs to be trusted by the API Gateway
        - This certificate does not need to be trusted by anyone else

- **Discovery Service**
    - Provides an HTTPS port
    - Has a keystore or SAF keyring with a server certificate
    - Has a truststore or SAF keyring that contains certificates needed to trust API services

- **API Service**
    - Provides an HTTPS port
    - Has a keystore or SAF keyring with a server and client certificate
        - The server certificate needs to be trusted by the Gateway
        - The client certificate needs to be trusted by the Discovery Service
        - The client and server certificates can be the same
        - These certificates do not need to be trusted by anyone else
    - Has a truststore or SAF keyring that contains one or more certificates that are required to trust the Gateway and Discovery Service

## Setting ciphers for API ML services

You can override ciphers that are used by the HTTPS servers in API ML services by configuring properties of the Gateway, Discovery Service, and API Catalog.

:::note
You do not need to rebuild JAR files when you override the default values in shell scripts.
:::

The default value for each service can be found in the [application.yml](../gateway-service/src/main/resources/application.yml) file. The default configuration is packed in .jar files. On z/OS, you can override the default configuration in `<RUNTIME_DIR>/components/<APIML_COMPONENT>/bin/start.sh`.
Add the launch parameter of the shell script to set a cipher:

```
-Dapiml.security.ciphers=<cipher-list>
```

On localhost, you can override the default configuration in [config/local/gateway-service.yml](../config/local/gateway-service.yml) (including other YAML files for development purposes).
 
The following list shows the default ciphers. API ML services use the following cipher order:

:::note
Ensure that the version of Java you use is compatible with the default cipherset.
:::

```
   TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
   TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
   TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
   TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384
```

Only IANA ciphers names are supported. For more information, see [Cipher Suites](https://wiki.mozilla.org/Security/Server_Side_TLS#Cipher_suites) or [List of Ciphers](https://testssl.net/openssl-iana.mapping.html).

## JSON Web Token (JWT)

The API ML authentication token in the form of JWT is signed with an asymmetric private key that is generated during Zowe keystore configuration. The JWT is signed with the RS256 signature algorithm.

You can find this private key under the alias `localhost`, in the PKCS12 keystore that is stored in `${KEYSTORE_DIRECTORY}/localhost/localhost.keystore.p12`. The public key necessary to validate the JWT signature is read from the keystore.

You can also use the `/gateway/api/v1/auth/keys/public` endpoint to obtain a public key that can be used to verify the signature of the JWT in standard [JWK format](https://openid.net/specs/).

## z/OSMF JSON Web Tokens Support

Your z/OSMF instance can be enabled to support JWTs as described in [Enabling JSON Web Token support](https://www.ibm.com/support/knowledgecenter/SSLTBW_2.4.0/com.ibm.zos.v2r4.izua300/izuconfig_EnableJSONWebTokens.htm).
In such cases, the Zowe API ML uses this JWT and does not generate its own Zowe JWT. All authentication APIs, such as `/gateway/api/v1/login` and `/gateway/api/v1/check` function in the same way as without z/OSMF JWT.
The Gateway service endpoint `/gateway/api/v1/auth/keys/public` serves the z/OSMF JWK that can be used for JWT signature validation.
