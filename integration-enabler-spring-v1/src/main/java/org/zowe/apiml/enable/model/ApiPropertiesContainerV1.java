/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

/**
 * Retrieve information about this service from application.yml, used to send information to the apiDoc discovery service
 */
@Component
@ConditionalOnProperty(prefix = "eureka.instance.metadata-map.mfaas.discovery", value = "enableApiDoc", havingValue = "true", matchIfMissing = true)
@ConfigurationProperties("eureka.instance.metadata-map.mfaas.api-info")
@Data
@Slf4j
public class ApiPropertiesContainerV1 {

    private Map<String, ApiProperties> apiVersionProperties = new HashMap<>();

    @PostConstruct
    public void displayConfiguredInformation() {
        String toString = apiVersionProperties.toString().replace(",", ",\n");

        log.trace("===========================================");
        log.trace("=    CONFIGURED API HEADER INFORMATION ");
        log.trace("=\n");
        log.trace(toString);
        log.trace("===========================================");
    }

    @Data
    public static class ApiProperties {

        private String title;

        private String description;

        private String version;

        private String basePackage;
        private String apiPattern;
        private String groupName;
    }
}
