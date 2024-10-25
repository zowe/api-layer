## Onboarding Python enabler for Zowe API Mediation Layer

This is the onboarding Python enabler for [Zowe API Mediation Layer](https://github.com/zowe/api-layer) (part of [Zowe](https://zowe.org)) that allows to register a Python based service to the API Mediation Layer Discovery Service. It uses [py-eureka-client](https://pypi.org/project/py-eureka-client/).

### How to use

1. Install



2. Inside your 

    ```python
  
    
    ```
   To make sure that your application will automatically unregister from Eureka once shut down, you can use the `unregister()` function, like shown in the example below.
   **Example:**

    ```python
    
        
    ```

3. Create a yaml file named `service-configuration.yml`, add the configuration properties and place the yaml file inside the root directory at the same level of your `app.py`.
   Below is an example of the configuration.

   **Example:**

    ```yaml
    # needs to be configured properly where yaml is parsed with pyyaml

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

4. Start your Python service and verify that it registers to the Zowe API Mediation Layer.
