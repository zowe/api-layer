# Sample Spring service

This is a sample Spring Hello world application using 'integration-enabler-java' enabler.

# How to Run 

You can start the service using:

    ./gradlew helloworld-jersey:tomcatRun

For more information read [docs/local-configuration.md](docs/local-configuration.md). 

# How to use

You can see this application registered to catalog on the tile "Sample API Mediation Layer Applications".

For API requests, use endpoints "/greeting" for a generic greet or "greeting/{name}" for a greet returning your input {name}.
