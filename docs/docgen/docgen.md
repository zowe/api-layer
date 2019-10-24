### Simple generation of documentation from yaml message files for Api mediation layer

#### Source:

https://github.com/jandadav/yaml-to-md

#### Usage:

java -jar Docgen-1.0.jar <PATH_TO_CONFIG_YAML>

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

#### Example message file:

https://github.com/jandadav/yaml-to-md/blob/master/config/testMessages.yml
