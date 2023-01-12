# API Catalog Services

## Standalone mode

Standalone mode allows displaying, without the need for authentication, services
that are stored on the disk. Standalone API Catalog does not connect to any
other service.

### Configuration

 - `apiml.catalog.standalone.enabled` 
    specifies whether to enable the standalone mode
    Default: false 
 - `apiml.catalog.standalone.servicesDirectory`
    specifies a directory where service definitions are stored
    Default: services

### Service directory structure

The service directory contains definitions of services that are visible in 
the Catalog. 

It consists of the following subdirectories:
    - apps
    - apiDocs

#### Apps subdirectory

`apps` subdirectory contains files in JSON format that describes services. All
JSON files inside the directory are processed. The file name does not have any
specific format.

The file contains the Eureka Instance descriptor of an API ML conformant
service. It is serialized `com.netflix.discovery.shared.Applications` object. 
JSON `application` object contains a list of one or multiple services.

You can use endpoints in the following link to obtain the file content:

https://github.com/Netflix/eureka/wiki/Eureka-REST-operations

Example:
[service1.json](config/local/catalog-standalone-defs/apps/service1.json)

#### ApiDocs subdirectory

`apiDocs` subdirectory contains API documentation in JSON format for services.
All API ML supported formats are available, such as Swagger or openAPI. All JSON
files are processed.

The file contains supported API documentation for one service and version.

The file name has the following structure:

`{serviceId}_{version}_default.json`

or

`{serviceId}_{version}.json`

The `default` suffix is used for the API version that is displayed in
the Catalog as the default one. Make sure that it corresponds to the default
version specified in `apiInfo` section in the service metadata provided in `app`
subdirectory. Each service MUST have exactly one API documentation file with
a default suffix.

Note:
You can find more information about apiInfo at
https://docs.zowe.org/stable/extend/extend-apiml/onboard-plain-java-enabler#api-info

Example:
[service2_org.zowe v1.0.0_default.json](config/local/catalog-standalone-defs/apiDocs/service2_org.zowe v1.0.0_default.json)
