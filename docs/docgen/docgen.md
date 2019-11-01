### Simple generation of documentation from yaml message files for Api mediation layer

#### Source:

https://github.com/jandadav/yaml-to-md

#### Usage:

- Download and build the project from the github repository. 
- Place the compiled jar file into this directory.
- You can then build the documentation using the following command:
`java -jar Docgen-1.0.jar <PATH_TO_CONFIG_YAML>`
- The configuration file that is being used to generate API Mediation layer documentation is `config.yml` within this directory.

#### Example yaml config file:

This serves to define which files should be processed and where to output

```
messageFiles:
  - title: "Message File 1"
    file: C:\workspace\testMessages.yml
  - title: "Message File 2"
    file: C:\workspace\testMessages2.yml

outputFile: sampleOutput.md
```
