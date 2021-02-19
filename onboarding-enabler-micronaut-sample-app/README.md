#Micronaut enabler

### (Optional) Install Micronaut CLI
- [Quick start new server](https://docs.micronaut.io/latest/guide/index.html#quickStart)

Micronaut CLI can be used to create new project, it is similar to Spring initializr. When project already exists, doesn't need to be used.
with homebrew:
```
brew cask install micronaut-projects/tap/micronaut
```
or download binaries:
```
https://micronaut.io/download.html
```
#### Crete new project
```
mn create-app <project-name>
```

##How to run

### Build runnable jar
```
gradle shadowJar
```
### Run sample application with gradle
```
gradle run
```
### Run sample application with java
Prerequisite: build runnable jar with shadowJar task
```
java -jar onboarding-enabler-micronaut-sample-app/build/libs/micronaut-enabler-1.0.jar
```
## Start with http server

- [Micronaut Micronaut HTTP Server documentation](https://docs.micronaut.io/latest/guide/index.html#httpServer)
