<!-- omit in toc -->
# IDE Setup

- [IntelliJ Idea setup](#intellij-idea-setup)
  - [Code Development](#code-development)
  - [Running of the services](#running-of-the-services)
    - [If using IDEA Ultimate Edition 2023](#if-using-idea-ultimate-edition-2023)
    - [If using IDEA Community Edition](#if-using-idea-community-edition)
- [Visual Studio Code setup](#visual-studio-code-setup)

## IntelliJ Idea setup

Guidelines relevant for development of the API Mediation Layer in the IntelliJ IDEA.

Be aware that Idea contains 
[a bug since 2023.1.4](https://youtrack.jetbrains.com/issue/IDEA-323055/Gradle-with-GraalVM-buildtools-plugin-fails-to-import-on-2023.2-EAP-5).
This bug break reading Gradle model, and it is not possible to load the project correctly. To avoid this issue it is 
possible to disable parallel processing by setting `org.gradle.parallel` to `false` in the
[gradle.properties](../gradle.properties) file.

### Code Development

- Enable _Annotations processing_ if you haven't done so already (Just go to settings and search for 'annotation')
- Install Lombok plugin. Go to the plugins in the setting and look for the lombok plugin.
- Make sure that the Gradle JVM is set to the JDK 1.8. To set it go to the Settings->Build,Execution,Deployment->Build Tools->Gradle

### Running of the services

- These are the application main classes and their corresponding service names for configuration files:
    1. ApiCatalogApplication - api-catalog-service
    2. DiscoverableClientSampleApplication - discoverable-client
    3. DiscoveryServiceApplication - discovery-service
    4. EnablerV1SampleApplication - onboarding-enabler-spring-sample-app
    5. GatewayApplication - gateway-service
    6. MockServicesApplication - mock-services

- There are no configuration files for:
    1. CachingService
    2. MetricsServiceApplication

- If you plan on using the [Dummy Authentication Provider](https://docs.zowe.org/stable/extend/extend-apiml/authentication-for-apiml-services/#dummy-authentication-provider) open the ./config/local/gateway-service.yml file and update `apiml.security.auth.provider:` to have value  `dummy`

#### If using IDEA Ultimate Edition 2023

- Go to 'Services', it is available via alt+8 or on the bottom line of the IDEA.

For each of the available services:

1. Right click a service and select 'Edit Configuration' (or press Shift + F4 while the service is selected)
2. Check that there are no set Environment variables (if you do not see the field you can press Alt + E or click on Modify options and select the Environment variables option from the list)
    - For the Discovery service add Environment variable `spring.profiles.active` and it's value `https`
5. Then in the 'Override configuration properties' section (Alt + P or select it from the Modify options list again) add a new parameter `spring.config.additional-location` and its value `file:./config/local/{SERVICE_NAME}.yml` Replace SERVICE_NAME with the above service names.
6. Check that you are shortening the command line, Modify options -> Java -> Shorten command line and in the new field select JAR manifest.
7. Run the service

#### If using IDEA Community Edition

For each of the available services:

1. Run -> Edit configurations...
2. Create New (Ctrl + N or Cmd + N) -> Application
3. Choose main class (E.g `org.zowe.apiml.gateway.GatewayApplication`)
4. Add Environment variable `spring.config.additional-location` and it's value `file:./config/local/{SERVICE_NAME}.yml` Replace SERVICE_NAME with respective main class (E.g `file:./config/local/gateway-service.yml`)
    - For the Discovery service add Environment variable `spring.profiles.active` and it's value `https`
5. Run the service Run -> Run... (Alt + Shift + F10 or Ctrl + Option + R)

Repeat the above for the following application main classes and their service names as written above:

- gateway-service
- discovery-service
- mock-services
- api-catalog-services
- discoverable-client

## Visual Studio Code setup

You can use Visual Studio Code to develop and debug.

**Follow these steps:**

1. Install the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack), [Spring Boot Extension Pack](https://marketplace.visualstudio.com/items?itemName=Pivotal.vscode-boot-dev-pack) and [Lombok Annotations Support for VS Code](https://marketplace.visualstudio.com/items?itemName=GabrielBB.vscode-lombok) extensions.
    - The VSCode Java Extension requires Java 11, which would need to be installed alongside with Java 8 (so long as APIML runs on Java 8). The VSCode `java.home` setting would need to point to the Java 11 installation.
    - So long as APIML runs on Java 8, it is required that the `JAVA_HOME` environment variable points to Java 8 by default. This way, with the above-mentioned VSCode setting, the java extension will work fine with Java 11 while the debugger and the test runner will use Java 8.
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
5. To debug unit tests, use the shortcuts that are created by the Java Test Runner extension.

Example to launch Gateway application in `launch.json`:

```json
{
    "type": "java",
    "name": "Launch GatewayApplication",
    "request": "launch",
    "mainClass": "org.zowe.apiml.gateway.GatewayApplication",
    "projectName": "gateway-service",
    "vmArgs": [
        "-Dspring.config.additional-location=file:../config/local/gateway-service.yml",
        "-Dspring.profiles.active=https"
    ],
    "args": [
        "--apiml.security.ssl.verifySslCertificatesOfServices=true",
        "--server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
        "--server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12",
        "--server.internal.ssl.keyStore=../keystore/localhost/localhost-multi.keystore.p12",
        "--apiml.security.auth.provider=dummy"
    ]
}
```
