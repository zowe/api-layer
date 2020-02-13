/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.apidoc.model;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * Retrieve information about this service from application.yml, used to send information to the apiDoc discovery service
 */
@Component
@ConditionalOnProperty(prefix = "eureka.instance.metadata-map.apiml.service.catalog", value = "enableApiDoc", havingValue = "true", matchIfMissing = true)
@ConfigurationProperties("eureka.instance.metadata-map.apiml.service")
@Data
public class ApiPropertiesContainer {

    private Map<String, ApiProperties> apiVersionProperties = new HashMap<>();

    @Data
    public static class ApiProperties {

        @NotBlank
        private String title;

        @NotBlank
        private String description;

        @NotBlank
        private String version;

        private String basePackage;
        private String apiPattern;
        private String groupName;
    }
}
