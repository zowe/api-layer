# Sample Spring service

This is a sample Spring Hello World application using the 'integration-enabler-java' enabler.

# How to Run 

You can start the service using the following command:

    ./gradlew helloworld-spring:tomcatRun

For more information, see [docs/local-configuration.md](docs/local-configuration.md). 

# How to use

You can see this application registered with the Catalog on the tile "Sample API Mediation Layer Applications".

For API requests, use the endpoints "/greeting" for a generic greeting or "greeting/{name}" for a greeting returning your input {name}.
