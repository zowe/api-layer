# Visual Studio Code setup

You can use Visual Studio Code to develop and debug.

**Follow these steps:**

1. Install the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack), [Spring Boot Extension Pack](https://marketplace.visualstudio.com/items?itemName=Pivotal.vscode-boot-dev-pack) and [Lombok Annotations Support for VS Code](https://marketplace.visualstudio.com/items?itemName=GabrielBB.vscode-lombok) extensions.
    * The VSCode Java Extension requires Java 11, which would need to be installed alongside with Java 8 (so long as APIML runs on Java 8). The VSCode `java.home` setting would need to point to the Java 11 installation.
    * So long as APIML runs on Java 8, it is required that the `JAVA_HOME` environment variable points to Java 8 by default. This way, with the above mentioned VSCode setting, the java extension will work fine with Java 11 while the debugger and the test runner will use Java 8. 
2. Run `gradlew eclipse` to generate the build information.
3. Add the following definitions to the `launch.json` file in the workspace:
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
4. Launch either of the services in debug mode (see `package.json` for launch scripts in debug mode). Then attach the debugger by running the appropriate launch configuration.
5. To debug unit tests, use the shortcuts that are created by the Java Test Runner exception.
