# Enabling PassTicket creation for API Services that Accept PassTickets

**Note**: This is a draft documentation that needs to be migrated to <https:/github.com/zowe/docs-site> after the functionality is completed.

- [Enabling PassTicket creation for API Services that Accept PassTickets](#enabling-passticket-creation-for-api-services-that-accept-passtickets)
  - [How to Enable PassTicket Support](#how-to-enable-passticket-support)
    - [Enable the Zowe started task user ID to generate PassTickets for the API service](#enable-the-zowe-started-task-user-id-to-generate-passtickets-for-the-api-service)
      - [ACF2](#acf2)
      - [RACF](#racf)
      - [TopSecret](#topsecret)
    - [API Services that Register Dynamically into API ML](#api-services-that-register-dynamically-into-api-ml)
    - [API Services that Register Dynamically into API ML but not Provide Metadata](#api-services-that-register-dynamically-into-api-ml-but-not-provide-metadata)
    - [API Services that are Defined using Static YAML Definition](#api-services-that-are-defined-using-static-yaml-definition)
  - [What Developers Need to with API Services that Register Dynamically into API ML](#what-developers-need-to-with-api-services-that-register-dynamically-into-api-ml)

The API Mediation Layer provides transparent authentication using PassTickets for API service that accept them.

It means that API clients can use the Zowe JWT obtained from [authentication endpoint](https://docs.zowe.org/stable/extend/extend-apiml/api-mediation-security.html#authentication-for-api-ml-services) of the API gateway to access such API service even if the API service
does not support the JWT token.

If the API client provide a valid Zowe JWT token, the API gateway generates a valid PassTicket and uses that to access the API service.
The user ID and password are provided in the Authorization header of the HTTP requests using the
[Basic authentication scheme](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication#Basic_authentication_scheme).

## How to Enable PassTicket Support

You need to do following for each service that requires PassTickets:

- Ensure that PassTickets are enabled for every user who might require access to API service
  - Follow documentation of your API service that explains how to active support for PassTickets
  - Remember the value of the APPLID of the API service
- Enable the Zowe started task user ID to generate PassTickets for the API service
- Enable the PassTicket support in the API gateway for your API service

### Enable the Zowe started task user ID to generate PassTickets for the API service

Following variables are used in the commands:

- `<applid>` is the APPLID value that is used by the API service for PassTicket support (e.g. `OMVSAPPL`)

- `<zowesrv>`is Zowe started task user ID permission

#### ACF2

Grant the Zowe started task user ID permission to generate PassTickets for users of that API service. For example:

```txt
ACF
SET RESOURCE(PTK)
RECKEY IRRPTAUTH ADD(<applid>.- UID(<zowesrv>) SERVICE(UPDATE,READ) ALLOW)
F ACF2,REBUILD(PTK),CLASS(P)
END
```

#### RACF

To enable PassTicket creation for API service users, define the profile `IRRPTAUTH.<applid>.*` in the `PTKTDATA` class and set the universal access authority to NONE and grant the Zowe started task user ID permission to generate PassTickets for users of that API service. For example:

```txt
RDEFINE PTKTDATA IRRPTAUTH.<applid>.* UACC(NONE)
PERMIT IRRPTAUTH.<applid>.* CL(PTKTDATA) ID(<zowesrv>) ACCESS(UPDATE)
SETROPTS RACLIST(PTKTDATA) REFRESH
```

#### TopSecret

Grant the Zowe started task user ID permission to generate PassTickets for users of that API service. For example:

```txt
TSS PERMIT(<zowesrv>) PTKTDATA(IRRPTAUTH.<applid>.) ACCESS(READ,UPDATE)
TSS REFRESH
```

### API Services that Register Dynamically into API ML

The API services that support Zowe API Mediation Layer and use dynamic registration into Discovery Service provide metadata
that enable PassTicket support.

As a user of the API you are not require to do anything in this case. All required information is provided by the

### API Services that Register Dynamically into API ML but not Provide Metadata

Some services that can use PassTickets do not provide the metadata yet. For such service you can provide them
extenally in the same files as for the static YAML definitons.

Add following section to a YAML file with static definition.

```yaml
additionalServiceMetadata:
    <serviceId>:
        authenticationScheme: httpBasicPassTicket
        applid: <applid>
```

`<serviceId>` is the service ID of the service where you want to add metadata.

### API Services that are Defined using Static YAML Definition

Add the following metadata to the same level as the `serviceId`, for example:

```yaml
    - serviceId: ...
      authenticationScheme: httpBasicPassTicket
      applid: TSTAPPL
```

The fields are explained below.

## What Developers Need to with API Services that Register Dynamically into API ML

As a developer of this application, you need to provide additional metadata.
This metadata tell API gateway that it needs to use PassTickets and how to generate them.

```yaml
authenticationScheme: httpBasicPassTicket
applid: <applid>
```

`httpBasicPassTicket` is the value that means that HTTP Basic authentication schmeme is used with PassTickets.

`<applid>` if the APPLID value that is used by the API service for PassTicket support (e.g. `OMVSAPPL`).

The other values of `authenticationScheme` that supported:

- `bypass` (default) - API gateway does not modify authentication headers for the API service.
- `zoweJwt` - The Zowe JWT token is expected. API gateway does not modify but can process it.
