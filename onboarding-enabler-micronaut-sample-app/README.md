## Feature http-client documentation

- [Micronaut Micronaut HTTP Client documentation](https://docs.micronaut.io/latest/guide/index.html#httpClient)

###(Optional) Install Micronaut CLI
Micronaut CLI can be used to create new project, it is similar to Spring initializr. When project already exists, doesn't need to be used.
with homebrew:
```
brew cask install micronaut-projects/tap/micronaut
```
download binaries:
```
https://micronaut.io/download.html
```

### Build runnable jar
```
gradle shadowJar
```
##How to run

### Run sample application with gradle
```
gradle run
```
### Run sample applicat`zion with java
Prerequisite: build runnable jar with shadowJar task
```
java -jar onboarding-enabler-micronaut-sample-app/build/libs/micronaut-enabler-1.0.jar
```
