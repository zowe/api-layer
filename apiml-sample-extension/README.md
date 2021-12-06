# API ML sample extension

This is an API ML sample extension. It only contains a simple controller for testing.
The extension is added to the API Gateway class path. Therefore, as a result, the controller is added in the context 
of the API Gateway without starting a new service.

## Usage

If the extension is correctly added to the API Gateway classpath, it will be possible to 
call the REST endpoint defined in the controller via Gateway.
The extension is scanned and added to the classpath during the Zowe instance preparation, therefore
once the Gateway is started, you can:

1. Call the `https://<hostname>:<gatewayPort>/api/v1/greeting` endpoint though Gateway
2. Verify that you get the message `Hello, I'm a sample extension!` as response
