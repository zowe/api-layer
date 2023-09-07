# Diagnostics

The running services need to be diagnosed when there are problems. Spring Boot Actuator provides a lot of useful REST API endpoints to know more about the application state.
The issue is that they should not be opened to public.

The default configuration exposes just few endpoints that are safe. For local development a profile `diag` is turned on to enable all of them. 

See `config/local/gateway-service.yml` file and look for:

    spring.profiles.include: diag


Actuator endpoints can be accessed on `/application/` URL for each service. 
For example, the Gateway on localhost has this URL:

    https://localhost:10010/application/


## Build Information

It can be useful to know what code is used. This is available at `/application/info` endpoint for each service. E.g:

    https://localhost:10010/application/info

## Version Information

It is also possible to know the version of API ML and Zowe (if API ML used as part of Zowe), using the `/gateway/api/v1/version` endpoint in the API Gateway service. E.g.:

    https://localhost:10010/gateway/api/v1/version

To view the Zowe version requires setting up the launch parameter of API Gateway - `apiml.zoweManifest` with a path to the Zowe build manifest.json file, which is usually located in the root folder of Zowe build. 
If the encoding of manifest.json file is different from UTF-8 and IBM-1047 it requires setting up the launch parameter of API Gateway - `apiml.zoweManifestEncoding` with the correct encoding.
    
Example of response:

    {
        "zowe": {
            "version": "1.8.0",
            "buildNumber": "437",
            "commitHash": "8dd9f512a2723bc07840c193d78e5d5ff5751e92"
        },
        "apiml": {
            "version": "1.3.3-SNAPSHOT",
            "buildNumber": "10",
            "commitHash": "c50d0b5"
        }
    }                                                  
