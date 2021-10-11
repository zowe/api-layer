# Plain Java Enabler Sample
✌( ͡° ͜ʖ ͡°)✌ "Superplain" edition

It's so _LIGHT_ that it doesn't have any _REST_ endpoints.

### Quick start

- run
    
    `java -jar <jar location> <path to configuration>`

- run against local api-layer instance from api-layer repository root
    
    `java -jar ./onboarding-enabler-java-sample-app-plain-java/build/libs/sample.jar ./onboarding-enabler-java-sample-app-plain-java/config/service-configuration.yml`

- run on mainframe (assuming artefacts in the same folder, with keyrings)

    `java -Xquickstart -Dfile.encoding=UTF-8 -Djava.protocol.handler.pkgs=com.ibm.crypto.provider -jar sample.jar service-configuration.yml`

### Configuration
Configuration sample can be found in the `config` folder. The config is for local api layer instance, running from repository root. Customize and use as you see fit.
