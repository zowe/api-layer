package com.ca.mfaas.message.yaml;

public class YamlMessageServiceInstance {

    private static YamlMessageService yamlMessageService;

    private YamlMessageServiceInstance() {
    }

    public static YamlMessageService getInstance() {
        if (yamlMessageService == null) {
            yamlMessageService =  new YamlMessageService();
        }

        return yamlMessageService;
    }
}
