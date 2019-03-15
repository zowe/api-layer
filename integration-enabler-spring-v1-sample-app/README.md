# Sample Spring Boot v1 service

This is a sample Helloworld application using the 'integration-enabler-spring-v1'.

#How to run

You can start the service using:

```shell
java -jar integration-enabler-spring-v1-sample-app/build/libs/enabler-springboot-1.5.9.RELEASE-sample.jar --spring.config.location=classpath:/,file:./config/local/integration-enabler-spring-v1-sample-app.yml
```

# How to use

You can see this application registered to catalog on the tile "Sample API Mediation Layer Applications".

For API requests, use endpoints "/greeting" for a generic greet or "greeting/{name}" for a greet returning your input {name}.
