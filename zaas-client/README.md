# zaas-client

- [zaas-client](#zaas-client)
  - [Introduction](#introduction)
  - [Pre-requisites](#pre-requisites)
  - [Getting Started](#getting started)
  - [Building](#building)
  - [Packaging](#packaging)
  - [Required Security Access for Development](#required security access for development)
  - [Commands to Setup PassTickets for Your Service](#commands to setup passtickets for your service)
  
## Introduction

This is a native java library developed on the top of APIML login, query and pass ticket API. It is developed with apache http Client version 4.5.11.

## Pre-requisites

1) Java SDK version 1.8.
2) Gateway Service of APIML layer should be up and running as a service.
3) Property file which defines the keystore or truststore certificates.

### Getting Started

1) In order to use this library you can create your API(RestController in case of Spring API) for login, query and pass ticket.

2) Then add zaas-client as a dependency in your project. This library provides you the following interface:

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
3) In order to use zaas-client, you need to provide a property file to initialize ConfigProperties used 
in the token service which includes the path to your truststore and keystore files and their following 
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

4)'zaas-client' allows you to have following functionality in your application:

a) Login:
In order to integrate login you just have to call either of the method provided for login in the 'TokenService' interface
based on how you want the user to provide the credentials.

If user provides credentials in the request body then you can call the following method from your API:
```java
String login(String userId, String password) throws ZaasClientException;
 ``` 
In case, user is providing credentials as Basic Auth then following method can be used:
```java
String login(String authorizationHeader) throws ZaasClientException;
 ```    
These methods will return the JWT token in return as a String. This token can be further used to authenticate the user
in rest of the API's.

This method will automatically use the truststore file to add a security layer which you have configured using ConfigProperties class.

b) Query:
Query method is used to provide you the details embedded in the token which includes creation time of the token, expiration time 
of the token and the user to whom the token has been issued and so on.

To use this method simply call this method from your API.
```java
ZaasToken query(String token) throws ZaasClientException;
 ``` 
In return you will get the 'ZaasToken' Object which has the following JSON format.

This method will automatically use the truststore file to add a security layer which you have configured using ConfigProperties class.

c) Pass Ticket:
Pass ticket method has an added layer of protection. In order to use it just call the method of the interface and provide
a valid APPLID of the application and JWT token as an input.

The APPLID is a name of the application (up to 8 characters)that is used by security products to differentiate 
certain security operations (like PassTickets) between applications.

It has an added layer of security where you do not have to provide an input to the method but since you have already initialized the
ConfigProperties class. Hence, it will automatically fetch the truststore and keystore files as an input.

In return, this method will provide a valid pass ticket as a String to the authorized user.

#### Building


#### Packaging


## Required Security Access for Development


### Commands to Setup PassTickets for Your Service

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

`<key>` is the value of 16 hexadecimal digits (creating an 8-byte or 64-bit key). For example: `FEDCBA9876543210`.
