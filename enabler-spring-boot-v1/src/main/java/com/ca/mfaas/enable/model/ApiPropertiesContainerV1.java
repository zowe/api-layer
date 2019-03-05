/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.model;

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
public class ApiPropertiesContainerV1 {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ApiPropertiesContainerV1.class);
    private Map<String, ApiProperties> apiVersionProperties = new HashMap<>();

    public ApiPropertiesContainerV1() {
    }

    @PostConstruct
    public void displayConfiguredInformation() {
        String toString = apiVersionProperties.toString().replaceAll(",", ",\n");

        log.trace("===========================================");
        log.trace("=    CONFIGURED API HEADER INFORMATION ");
        log.trace("=\n");
        log.trace(toString);
        log.trace("===========================================");
    }

    public Map<String, ApiProperties> getApiVersionProperties() {
        return this.apiVersionProperties;
    }

    public void setApiVersionProperties(Map<String, ApiProperties> apiVersionProperties) {
        this.apiVersionProperties = apiVersionProperties;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.enable.model.ApiPropertiesContainerV1)) return false;
        final com.ca.mfaas.enable.model.ApiPropertiesContainerV1 other = (com.ca.mfaas.enable.model.ApiPropertiesContainerV1) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$apiVersionProperties = this.apiVersionProperties;
        final java.lang.Object other$apiVersionProperties = other.apiVersionProperties;
        if (this$apiVersionProperties == null ? other$apiVersionProperties != null : !this$apiVersionProperties.equals(other$apiVersionProperties))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.enable.model.ApiPropertiesContainerV1;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $apiVersionProperties = this.apiVersionProperties;
        result = result * PRIME + ($apiVersionProperties == null ? 43 : $apiVersionProperties.hashCode());
        return result;
    }

    public String toString() {
        return "ApiPropertiesContainerV1(apiVersionProperties=" + this.apiVersionProperties + ")";
    }

    public static class ApiProperties {

        private String title;

        private String description;

        private String version;

        private String basePackage;
        private String apiPattern;
        private String groupName;

        public ApiProperties() {
        }

        public String getTitle() {
            return this.title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return this.version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getBasePackage() {
            return this.basePackage;
        }

        public void setBasePackage(String basePackage) {
            this.basePackage = basePackage;
        }

        public String getApiPattern() {
            return this.apiPattern;
        }

        public void setApiPattern(String apiPattern) {
            this.apiPattern = apiPattern;
        }

        public String getGroupName() {
            return this.groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof com.ca.mfaas.enable.model.ApiPropertiesContainerV1.ApiProperties)) return false;
            final com.ca.mfaas.enable.model.ApiPropertiesContainerV1.ApiProperties other = (com.ca.mfaas.enable.model.ApiPropertiesContainerV1.ApiProperties) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$title = this.title;
            final java.lang.Object other$title = other.title;
            if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
            final java.lang.Object this$description = this.description;
            final java.lang.Object other$description = other.description;
            if (this$description == null ? other$description != null : !this$description.equals(other$description))
                return false;
            final java.lang.Object this$version = this.version;
            final java.lang.Object other$version = other.version;
            if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
            final java.lang.Object this$basePackage = this.basePackage;
            final java.lang.Object other$basePackage = other.basePackage;
            if (this$basePackage == null ? other$basePackage != null : !this$basePackage.equals(other$basePackage))
                return false;
            final java.lang.Object this$apiPattern = this.apiPattern;
            final java.lang.Object other$apiPattern = other.apiPattern;
            if (this$apiPattern == null ? other$apiPattern != null : !this$apiPattern.equals(other$apiPattern))
                return false;
            final java.lang.Object this$groupName = this.groupName;
            final java.lang.Object other$groupName = other.groupName;
            if (this$groupName == null ? other$groupName != null : !this$groupName.equals(other$groupName))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof com.ca.mfaas.enable.model.ApiPropertiesContainerV1.ApiProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $title = this.title;
            result = result * PRIME + ($title == null ? 43 : $title.hashCode());
            final java.lang.Object $description = this.description;
            result = result * PRIME + ($description == null ? 43 : $description.hashCode());
            final java.lang.Object $version = this.version;
            result = result * PRIME + ($version == null ? 43 : $version.hashCode());
            final java.lang.Object $basePackage = this.basePackage;
            result = result * PRIME + ($basePackage == null ? 43 : $basePackage.hashCode());
            final java.lang.Object $apiPattern = this.apiPattern;
            result = result * PRIME + ($apiPattern == null ? 43 : $apiPattern.hashCode());
            final java.lang.Object $groupName = this.groupName;
            result = result * PRIME + ($groupName == null ? 43 : $groupName.hashCode());
            return result;
        }

        public String toString() {
            return "ApiPropertiesContainerV1.ApiProperties(title=" + this.title + ", description=" + this.description + ", version=" + this.version + ", basePackage=" + this.basePackage + ", apiPattern=" + this.apiPattern + ", groupName=" + this.groupName + ")";
        }
    }
}
