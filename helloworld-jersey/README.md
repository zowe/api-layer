# Sample Java Jersey service

This is a sample Hello World application using Java Jersey that uses the 'onboarding-enabler-java' enabler.

# How to Run 

You can start the service using the following command:

    ./gradlew helloworld-jersey:tomcatRun

For more information, see [docs/local-configuration.md](docs/local-configuration.md).

# How to use

You can see this application registered to the Catalog under the tile "Sample API Mediation Layer Applications".

For an API request, use endpoints "/greeting" for a generic greeting or "greeting/{name}" for a greeting returning your input {name}.
