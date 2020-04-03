# zaas-client

- [zaas-client](#zaas-client)
  - [Introduction](#introduction)
  - [Pre-requisites](#pre-requisites)
  - [Getting Started](#getting-started)
  - [Commands to Setup PassTickets for Your Service](#commands-to-setup-passtickets-for-your-service)
  
## Introduction

This is a native java library developed on the top of API ML login, query and pass ticket API. It is developed with apache http Client version 4.5.11.

## Pre-requisites

- Java SDK version 1.8.
- The Gateway Service of API ML should be up and running as a service.
- A property file which defines the keystore or truststore certificates.

### Getting Started

To use this library use the procedure described in this article.

**Follow these steps:**

1. Create your API (or RestController, in case of Spring API) for login, query and pass ticket.

2. Add the zaas-client as a dependency in your project. This library provides you the following interface:

```java
public interface TokenService {
    static String COOKIE_PREFIX = "apimlAuthenticationToken";
    void init(ConfigProperties configProperties);
    String login(String userId, String password) throws ZaasClientException;
    String login(String authorizationHeader) throws ZaasClientException;
    ZaasToken query(String token) throws ZaasClientException;
    String passTicket(String jwtToken, String applicationId) throws ZaasClientException;
}
```
3. To use `zaas-client`, provide a property file to initialize `ConfigProperties` used 
in the token service. Include the path to your truststore and keystore files and the following 
configuration parameters:

```java
public class ConfigProperties {

    private String apimlHost;
    private String apimlPort;
    private String apimlBaseUrl;
    private String keyStoreType;
    private String keyStorePath;
    private String keyStorePassword;
    private String trustStoreType;
    private String trustStorePath;
    private String trustStorePassword;
}
```
## Functionalities of `zaas-client`

`zaas-client` enables your application with the following functionalities:

- **Login**

  To integrate login, call one of the following methods for login in the `TokenService` interface. Credentials can be provided either in   the request body, or as Basic Auth.

    - If user provides credentials in the request body, then you can call the following method from your API:
    ```java
    String login(String userId, String password) throws ZaasClientException;
    ``` 
    - If the user is providing credentials as Basic Auth, then use the following method:
    ```java
    String login(String authorizationHeader) throws ZaasClientException;
    ```    
    These methods will return the JWT token as a String. This token can be further used to authenticate the user
    in subsequent APIs.

    This method will automatically use the truststore file to add a security layer, which you have configured in the `ConfigProperties`         class.

- **Query**

    The Query method is used to provide you the details embedded in the token which includes creation time of the token, expiration time 
    of the token, and the user to whom the token has been issued.

    To use this method, call the method from your API.
    ```java
    ZaasToken query(String token) throws ZaasClientException;
    ``` 
    In return you receive the `ZaasToken` Object in JSON format.

    This method will automatically use the truststore file to add a security layer, which you configured in the `ConfigProperties`         class.

- **Pass ticket**

    The Pass ticket method has an added layer of protection. To use this method, call the method of the interface and provide
    a valid APPLID of the application and JWT token as an input.

    The APPLID is the name of the application (up to 8 characters)vthat is used by security products to differentiate 
    certain security operations (like PassTickets) between applications.

    This method has an added layer of security so that you do not have to provide an input to the method as you have already initialized the
    `ConfigProperties` class. As such, this method automatically fetches the truststore and keystore files as an input.

    In return, this method provides a valid pass ticket as a String to the authorized user.

## Commands to set up PassTickets for your service

Commands for CA Top Secret for z/OS:

```tss
  /* Define PassTicket for APPLID <applid> without replay protection */
  TSS ADDTO(NDT) PSTKAPPL(<applid>) SESSKEY(<key>) SIGNMULTI
```

Commands for IBM RACF:

```racf
  /* Define APPLID <applid> that can be used by <srv> user */
  RDEFINE APPL <applid> UACC(NONE)
  PERMIT <applid> CL(APPL) ACCESS(NONE) ID(<srv>)
  SETROPTS RACLIST(APPL) REFRESH

  /* Activate PassTickets in RACF, if not activated */
  SETROPTS CLASSACT(PTKTDATA)
  SETROPTS RACLIST(PTKTDATA)
  SETROPTS GENERIC(PTKTDATA)

  /* Define PassTicket for APPLID <applid> without replay protection */
  RDEFINE PTKTDATA <applid> SSIGNON(KEYMASKED(<key>)) +
    APPLDATA('NO REPLAY PROTECTION') UACC(NONE)
  SETROPTS RACLIST(PTKTDATA) REFRESH
```

Commands for ACF2:

```acf2
SET RESOURCE(APL)
RECKEY <applid> ADD(UID(<user>) ALLOW)
F ACF2,REBUILD(APL)

SET PROFILE(PTKTDATA) DIVISION(SSIGNON)
INSERT <applid> SSKEY(<key>) MULT-USE
F ACF2,REBUILD(PTK),CLASS(P)
```
where:

**`<key>`**

is the value of 16 hexadecimal digits (creating an 8-byte or 64-bit key). 

**Example:** `FEDCBA9876543210`.
