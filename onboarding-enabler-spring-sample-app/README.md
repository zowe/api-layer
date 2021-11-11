# Sample Spring Boot Service

This is a sample Hello World application using the 'onboarding-enabler-spring'enabler.

## Prerequisite

* A built project is required before proceding.

## How to run

You can start the service using the following shell script:

```shell
java -jar onboarding-enabler-spring-sample-app/build/libs/enabler-springboot-2.3.9.RELEASE-sample.jar --spring.config.location=classpath:/,file:./config/local/onboarding-enabler-spring-v1-sample-app.yml
```

## How to use

You can see this application registered to the Catalog on the tile "Sample API Mediation Layer Applications".

For API requests, use the endpoints "/greeting" for a generic greeting, or "greeting/{name}" for a greeting returning your input {name}.
