# zaas-client

- [zaas-client](#zaas-client)
  - [Introduction](#introduction)
  - [Functionalities of `zaas-client`](#functionalities-of-zaas-client)
  - [Pre-requisites](#pre-requisites)
  - [Getting Started(Step by Step Instruction)](#getting-started)
  - [Commands to Set Up PassTickets for Your Service](#commands-to-set-up-passtickets-for-your-service)

## Introduction

This is a native java library developed on the top of API ML login, query and pass ticket API. It is developed with apache http Client version 4.5.11.

## Functionalities of zaas-client

This java library provides you the `ZaasClient` interface:

    ```java
    public interface ZaasClient {
        static String COOKIE_PREFIX = "apimlAuthenticationToken";
        void init(ConfigProperties configProperties);
        String login(String userId, String password) throws ZaasClientException;
        String login(String authorizationHeader) throws ZaasClientException;
        ZaasToken query(String token) throws ZaasClientException;
        String passTicket(String jwtToken, String applicationId) throws ZaasClientException;
    }
    ```

which enables your application to add following functions:

- **Obtain JWT token (login)**

  To integrate login, call one of the following methods for login in the `ZaasClient` interface. Credentials can be provided either in   the request body, or as Basic Auth.

  - If user provides credentials in the request body, then you can call the following method from your API:

    ```java
    String login(String userId, String password) throws ZaasClientException;
    ```

  - If the user is providing credentials as Basic Auth, then use the following method:

      ```java
      String login(String authorizationHeader) throws ZaasClientException;
      ```

  These methods will return the JWT token as a String. This token can be further used to authenticate the user in subsequent APIs.

  This method will automatically use the truststore file to add a security layer, which you have configured in the `ConfigProperties`         class.

- **Validate and get details from token (query)**

    The `query` method is used to provide you the details embedded in the token which includes creation time of the token, expiration time
    of the token, and the user to whom the token has been issued.

    To use this method, call the method from your API.

    ```java
    ZaasToken query(String token) throws ZaasClientException;
    ```

    In return you will receive the `ZaasToken` Object in JSON format.

    This method will automatically use the truststore file to add a security layer, which you configured in the `ConfigProperties`         class.

- **Obtain a PassTicket (passTicket)**

    The `passTicket` method has an added layer of protection. To use this method, call the method of the interface and provide
    a valid APPLID of the application and JWT token as an input.

    The APPLID is the name of the application (up to 8 characters) that is used by security products to differentiate certain security operations (like PassTickets) between applications.

    This method has an added layer of security so that you do not have to provide an input to the method as you have already initialized the
    `ConfigProperties` class. As such, this method automatically fetches the truststore and keystore files as an input.

    In return, this method provides a valid pass ticket as a String to the authorized user.

    For additional information about PassTickets in API ML see [Enabling PassTicket creation for API Services that Accept PassTickets](https://docs.zowe.org/stable/extend/extend-apiml/api-mediation-passtickets.html).

## Pre-requisites

- Java SDK version 1.8.
- The Gateway Service of API ML should be up and running as a service.
- A property file which defines the keystore or truststore certificates.

### Getting Started(Step by Step Instruction)

To use this library use the procedure described in this article.

**Follow these steps:**

1. Add `zaas-client` as a dependency in your project. 

    Gradle:
    
        dependencies {
            compile 'org.zowe.apiml.sdk:zaas-client:{{version}}'
        }

    Pom:
    
        <dependency>
                    <groupId>org.zowe.apiml.sdk:zaas-client</groupId>
                    <artifactId>{{version}}</artifactId>
        </dependency>

2. In your application, create your java class which will be used to create an instance of `ZaasClient` and further to use its method to
   login, query and to issue passTicket.

3. To use `zaas-client`, provide a property file for configuration. Kindly check `org.zowe.apiml.zaasclient.config.ConfigProperites` to make sure what properties we have to provide in the property file. 
 
    ```java
        public ConfigProperties getConfigProperties(){
           CofigProperties configProperties = new ConfigProperties();
           // Code to initialize configProperties variables from your property file/yml.
           return configProperties;
        }
   ```
    
   Example:
   
   ```java
   private static ConfigProperties getConfigProperties() {
   String CONFIG_FILE_PATH = "zaas-client/src/test/resources/configFile.properties";
         String absoluteFilePath = new File(CONFIG_FILE_PATH).getAbsolutePath();
         ConfigProperties configProperties = new ConfigProperties();
         Properties configProp = new Properties();
           try {
               if (Paths.get(absoluteFilePath).toFile().exists()) {
                    configProp.load(new FileReader(absoluteFilePath));
    
                    configProperties.setApimlHost(configProp.getProperty("APIML_HOST"));
                    configProperties.setApimlPort(configProp.getProperty("APIML_PORT"));
                    configProperties.setApimlBaseUrl(configProp.getProperty("APIML_BASE_URL"));
                    configProperties.setKeyStorePath(configProp.getProperty("KEYSTOREPATH"));
                    configProperties.setKeyStorePassword(configProp.getProperty("KEYSTOREPASSWORD"));
                    configProperties.setKeyStoreType(configProp.getProperty("KEYSTORETYPE"));
                    configProperties.setTrustStorePath(configProp.getProperty("TRUSTSTOREPATH"));
                    configProperties.setTrustStorePassword(configProp.getProperty("TRUSTSTOREPASSWORD"));
                    configProperties.setTrustStoreType(configProp.getProperty("TRUSTSTORETYPE"));
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            return configProperties;
        }   
   ``` 
   
4. Create an instance of `ZaasClient` in your class and provide the `configProperties` object like the following:

    ```java
    ZaasClient zaasClient = new ZaasClientHttps(getConfigProperties());
    ```
   
6. Now use any of the method from `ZaasClient` in your class. Like for login use the following code snippet:

    ```java
       String zaasClientToken = zaasClient.login("user", "user");
    ```

7. Following is an example of a `SampleZaasClientImplementation`:

    ```java
    public class SampleZaasClientImplementation {
    
        /**
         * This method is used to fetch token from zaasClient
         * @param username
         * @param password
         * @return
         */
        public String login(String username, String password) {
            try {
                ZaasClient zaasClient = new ZaasClientHttps(getConfigProperties());
                String zaasClientToken = zaasClient.login(username, password);
                //Use this token  in subsequent calls
            } catch (ZaasClientException exception) {
                System.out.println(exception.getErrorMessage());
            }
        }
    
        /**
         * Method to instantiate configuration properties for zaas-client
         * @return
         */
        private static ConfigProperties getConfigProperties() {
            String CONFIG_FILE_PATH = "zaas-client/src/test/resources/configFile.properties";
            String absoluteFilePath = new File(CONFIG_FILE_PATH).getAbsolutePath();
            ConfigProperties configProperties = new ConfigProperties();
            Properties configProp = new Properties();
            try {
                if (Paths.get(absoluteFilePath).toFile().exists()) {
                    configProp.load(new FileReader(absoluteFilePath));
    
                    configProperties.setApimlHost(configProp.getProperty("APIML_HOST"));
                    configProperties.setApimlPort(configProp.getProperty("APIML_PORT"));
                    configProperties.setApimlBaseUrl(configProp.getProperty("APIML_BASE_URL"));
                    configProperties.setKeyStorePath(configProp.getProperty("KEYSTOREPATH"));
                    configProperties.setKeyStorePassword(configProp.getProperty("KEYSTOREPASSWORD"));
                    configProperties.setKeyStoreType(configProp.getProperty("KEYSTORETYPE"));
                    configProperties.setTrustStorePath(configProp.getProperty("TRUSTSTOREPATH"));
                    configProperties.setTrustStorePassword(configProp.getProperty("TRUSTSTOREPASSWORD"));
                    configProperties.setTrustStoreType(configProp.getProperty("TRUSTSTORETYPE"));
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            return configProperties;
        }
    }
    ```


## Commands to Set Up PassTickets for Your Service

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
