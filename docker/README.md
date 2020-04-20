# Docker

## For the development use

The main purpose of the development image is to help the new developer set up 
their environment with as little pain as possible. The built image is available
via [Docker hub](https://hub.docker.com/repository/docker/jbalhar/api-layer-development)

The main goal of the image is to simplify running of all the types of the tests. To do so there is a script [_test](https://github.com/zowe/api-layer/blob/master/docker/development/_test) that can be run from anywhere in the system. 

To download and run the container it is necessary to install the [Docker Engine](https://www.docker.com/) first. The following commands start new instance of the container and log you into the bash with root privileges. The last line then updates the version of the api-layer and run the unit tests, integration tests and end to end tests. 

```
docker run --rm --name api-layer -d jbalhar/api-layer-development:1.0.0 /sbin/my_init
docker exec -it api-layer bash
_test
```

The Dockerfile to inspect if you want to setup your own environment is available in the [repository](https://github.com/zowe/api-layer/blob/master/docker/development/Dockerfile)
