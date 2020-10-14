# zOSMF Mock

The goal for this application is to provide mock for the external services to make sure that when we develop new pieces, that it will work well together with the expected behavior of the services. The main perceived benefit is in the local development and in CICD pipeline being run outside of the zOS environment.

## How do you run the zOSMF

It is a Spring Boot application. So you either add it as a run configuration and run it together with other services or the npm command to run API ML also runs the mock. 

Command to run full set of api-layer `npm run api-layer`

## Configuration properties

The Mock runs over the HTTPS with the keystore and truststore also used by the other API ML services. These parameters are set in the section server.ssl

As we mainly use zOSMF to authenticate the user, there are two properties in the file, which specify the behavior of the authentication. 

`zosmf.username` - Specifies which users are valid from the point of view of the zOSMF. Provide the comma separated list of usernames. It is case sensitive. 

`zosmf.password` - Specifies passwords for the users in the `zosmf.username`. The password is at the same position in the comma separated list. On top of that if the password contains PASS_TICKET it is accepted as valid Passticket regardless of password specified here. 


