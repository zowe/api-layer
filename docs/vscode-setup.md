# Visual Studio Code setup

If you want to use Visual Studio Code to develop and debug then follow these steps:

1. Install the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack), [Spring Boot Extension Pack](https://marketplace.visualstudio.com/items?itemName=Pivotal.vscode-boot-dev-pack) and [Lombok Annotations Support for VS Code](https://marketplace.visualstudio.com/items?itemName=GabrielBB.vscode-lombok) extensions.
2. Run `gradlew eclipse` to generate the build information.
1. Add the following definitions to the `launch.json` file in the workspace.
    ```json
    {
        "type": "java",
        "name": "Debug (Attach)-ApiCatalogApplication<api-catalog-services>",
        "request": "attach",
        "hostName": "localhost",
        "port": 5014,
        "projectName": "api-catalog-services"
    },
    {
        "type": "java",
        "name": "Debug (Attach)-GatewayApplication<gateway-service>",
        "request": "attach",
        "hostName": "localhost",
        "port": 5010,
        "projectName": "gateway-service"
    }
    ```
Launch either of the services in debug mode and then attach the debugger by running the appropriate launch configuration.