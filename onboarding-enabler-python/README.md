## Onboarding Python enabler for Zowe API Mediation Layer

This is the onboarding Python enabler for [Zowe API Mediation Layer](https://github.com/zowe/api-layer) (part of [Zowe](https://zowe.org)) that allows to register a Python based service to the API Mediation Layer Discovery Service. It uses [py-eureka-client](https://pypi.org/project/py-eureka-client/).

### Installation

Install the package using pip:

```shell
    pip install zowe-apiml-onboarding-enabler-python
```

### How to use

1. Import the Enabler in Your Python Service. Add the following code block to register your service with Eureka:

    **Example:**

    ```python
        from fastapi import FastAPI
        from zowe_apiml_onboarding_enabler_python.registration import PythonEnabler
    
        app = FastAPI()
        enabler = PythonEnabler(config_file="service-configuration.yml")
    
        @app.on_event("startup")
        def register_service():
            enabler.register()
    ```
    To make sure that your application will automatically unregister from Eureka once shut down, you can use the `unregister()` function, like shown in the example below.
    
    ```python
     @app.on_event("shutdown")
        def unregister_service():
            enabler.unregister()
    ```

2. Create a yaml file named `service-configuration.yml`, add the configuration properties and place the yaml file inside a `/config` directory at the same level of your `app.py`.
   Below is an example of the configuration.

   **Example:**

    ```yaml

    eureka:
       ssl: true
       host: localhost
       ipAddress: 127.0.0.1
       port: 10011
       servicePath: '/eureka'
       maxRetries: 30
       requestRetryDelay: 1000
       registryFetchInterval: 5
    
    
    instance:
       app: pythonservice
       scheme: https
       vipAddress: pythonservice
       instanceId: localhost:pythonservice:10018
       homePageUrl: https://localhost:10018/pythonservice
       hostName: 'localhost'
       ipAddr: '127.0.0.1'
       port: 10018
       securePort: 10018
       secureVipAddress: pythonservice
       statusPageUrl: https://localhost:10018/pythonservice/application/info
       healthCheckUrl: https://localhost:10018/pythonservice/application/health
       nonSecurePortEnabled': false
       securePortEnabled: true
    metadata:
       apiml.catalog.tile.id: 'cademoapps'
       apiml.catalog.tile.title: 'Sample Python Hello World'
       apiml.catalog.tile.description: 'Applications Hello'
       apiml.routes.api_v1.gatewayUrl: "api/v1"
       apiml.routes.api_v1.serviceUrl: "/pythonservice"
       apiml.apiInfo.0.apiId: org.zowe.pythonservice
       apiml.apiInfo.0.gatewayUrl: "api/v1"
       apiml.apiInfo.0.swaggerUrl: https://localhost:10018/pythonservice/apidoc
       apiml.apiInfo.0.version: 1.0.0
       apiml.service.title: 'Zowe Sample Python Service'
       apiml.service.description: 'Sample API services to demonstrate Python Onboarding Enabler'
    
    ssl:
       certificate: ../keystore/localhost/localhost.keystore.cer
       keystore: ../keystore/localhost/localhost.keystore.key
       caFile: ../keystore/localhost/localhost.pem
       keyPassword: password

    ```

3. Start your Python service and verify that it registers to the Zowe API Mediation Layer.
