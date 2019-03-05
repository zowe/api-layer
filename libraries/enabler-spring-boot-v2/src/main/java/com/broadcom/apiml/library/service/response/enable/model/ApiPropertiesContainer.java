/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.enable.model;

import org.slf4j.Logger;
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
@ConditionalOnProperty(prefix = "eureka.instance.metadata-map.mfaas.discovery", value = "enableApiDoc", havingValue = "true", matchIfMissing = true)
@ConfigurationProperties("eureka.instance.metadata-map.mfaas.api-info")
public class ApiPropertiesContainer {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ApiPropertiesContainer.class);
    private Map<String, ApiProperties> apiVersionProperties = new HashMap<>();

    public ApiPropertiesContainer() {
    }

    public Map<String, ApiProperties> getApiVersionProperties() {
        return this.apiVersionProperties;
    }

    public void setApiVersionProperties(Map<String, ApiProperties> apiVersionProperties) {
        this.apiVersionProperties = apiVersionProperties;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiPropertiesContainer)) return false;
        final ApiPropertiesContainer other = (ApiPropertiesContainer) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$apiVersionProperties = this.getApiVersionProperties();
        final Object other$apiVersionProperties = other.getApiVersionProperties();
        if (this$apiVersionProperties == null ? other$apiVersionProperties != null : !this$apiVersionProperties.equals(other$apiVersionProperties))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ApiPropertiesContainer;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $apiVersionProperties = this.getApiVersionProperties();
        result = result * PRIME + ($apiVersionProperties == null ? 43 : $apiVersionProperties.hashCode());
        return result;
    }

    public String toString() {
        return "ApiPropertiesContainer(apiVersionProperties=" + this.getApiVersionProperties() + ")";
    }

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

        public ApiProperties() {
        }

        public @NotBlank
        String getTitle() {
            return this.title;
        }

        public void setTitle(@NotBlank String title) {
            this.title = title;
        }

        public @NotBlank
        String getDescription() {
            return this.description;
        }

        public void setDescription(@NotBlank String description) {
            this.description = description;
        }

        public @NotBlank
        String getVersion() {
            return this.version;
        }

        public void setVersion(@NotBlank String version) {
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
            if (!(o instanceof ApiProperties)) return false;
            final ApiProperties other = (ApiProperties) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$title = this.getTitle();
            final Object other$title = other.getTitle();
            if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
            final Object this$description = this.getDescription();
            final Object other$description = other.getDescription();
            if (this$description == null ? other$description != null : !this$description.equals(other$description))
                return false;
            final Object this$version = this.getVersion();
            final Object other$version = other.getVersion();
            if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
            final Object this$basePackage = this.getBasePackage();
            final Object other$basePackage = other.getBasePackage();
            if (this$basePackage == null ? other$basePackage != null : !this$basePackage.equals(other$basePackage))
                return false;
            final Object this$apiPattern = this.getApiPattern();
            final Object other$apiPattern = other.getApiPattern();
            if (this$apiPattern == null ? other$apiPattern != null : !this$apiPattern.equals(other$apiPattern))
                return false;
            final Object this$groupName = this.getGroupName();
            final Object other$groupName = other.getGroupName();
            if (this$groupName == null ? other$groupName != null : !this$groupName.equals(other$groupName))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof ApiProperties;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $title = this.getTitle();
            result = result * PRIME + ($title == null ? 43 : $title.hashCode());
            final Object $description = this.getDescription();
            result = result * PRIME + ($description == null ? 43 : $description.hashCode());
            final Object $version = this.getVersion();
            result = result * PRIME + ($version == null ? 43 : $version.hashCode());
            final Object $basePackage = this.getBasePackage();
            result = result * PRIME + ($basePackage == null ? 43 : $basePackage.hashCode());
            final Object $apiPattern = this.getApiPattern();
            result = result * PRIME + ($apiPattern == null ? 43 : $apiPattern.hashCode());
            final Object $groupName = this.getGroupName();
            result = result * PRIME + ($groupName == null ? 43 : $groupName.hashCode());
            return result;
        }

        public String toString() {
            return "ApiPropertiesContainer.ApiProperties(title=" + this.getTitle() + ", description=" + this.getDescription() + ", version=" + this.getVersion() + ", basePackage=" + this.getBasePackage() + ", apiPattern=" + this.getApiPattern() + ", groupName=" + this.getGroupName() + ")";
        }
    }
}
