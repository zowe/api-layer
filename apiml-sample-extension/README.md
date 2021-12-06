# API ML sample extension

This is an API ML sample extension. It only contains a simple controller for testing.
The extension is added to the API Gateway class path. Therefore, as a result, the controller is added in the context 
of the API Gateway without starting a new service.

## Usage

If the extension is correctly added to the API Gateway classpath, it will be possible to 
call the `https://<hostname>:<gatewayPort>/api/v1/greeting` endpoint.

