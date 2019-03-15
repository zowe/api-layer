# Sample Java Jersey service

This is a sample Hello world application using Java Jersey using 'integration-enabler-java' enabler.

# How to Run 

You can start the service using:

    ./gradlew helloworld-jersey:tomcatRun

For more information read [docs/local-configuration.md](docs/local-configuration.md).

# How to use

You can see this application registered to catalog under the tile "Sample API Mediation Layer Applications".

For API request, use endpoints "/greeting" for a generic greet or "greeting/{name}" for a greet returning your input {name}.
