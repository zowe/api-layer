# API Mediation Layer in a docker container

In the end there are three articles:
 - Start developing the API ML
 - Verify that your API is working with the API ML - More technical / developer guy
 - Use the try-out functionality - A different primary user

The API Mediation Layer is available for development and testing packaged as a Docker container. 

There are two main reasons to use following Docker container:
1) You have an API and want to verify that you can onboard the API to the API Mediation Layer 
2) You want to contribute to the API Mediation Layer but don't want to set up a full development environment on your machine

To use the prepared container you need to [install the Docker Engine](https://docs.docker.com/install/) on the machine, where you want to run it.

## Verify onboarding of API

Just for running:
With volume for static configuration. 

```
docker run --rm --name api-layer -p 10010-10019:10010-10019 -d jbalhar/api-layer-development:1.0.1 /sbin/my_init
docker exec api-layer bash _run
```

At the moment the static onboarding isn't allowed. Only via direct call or via enablers. 

The services are available via:
localhost:10010
localhost:10011
localhost:10014

To verify via 

## For the development use

The main purpose of the development image is to help the new developer set up 
their environment with as little pain as possible. The built image is available
via [Docker hub](https://hub.docker.com/repository/docker/jbalhar/api-layer-development)

The main goal of the image is to simplify running of all the types of the tests. To do so there is a script [_test](https://github.com/zowe/api-layer/blob/master/docker/development/_test) that can be run from anywhere in the system. 

To download and run the container it is necessary to install the [Docker Engine](https://www.docker.com/) first. The following commands start new instance of the container and log you into the bash with root privileges. The last line then updates the version of the api-layer and run the unit tests, integration tests and end to end tests. 

```
docker run --rm --name api-layer -p 10010:10010 10011:10011 10012:10012 10013:10013 10014:10014 -d jbalhar/api-layer-development:1.0.1 /sbin/my_init
docker exec api-layer bash _test
```

The Dockerfile to inspect if you want to setup your own environment is available in the [repository](https://github.com/zowe/api-layer/blob/master/docker/development/Dockerfile)
