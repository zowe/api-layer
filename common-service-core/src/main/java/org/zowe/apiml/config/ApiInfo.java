/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one API provided by a service
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@SuperBuilder
public class ApiInfo {

    public ApiInfo(String apiId, String gatewayUrl, String version, String swaggerUrl, String documentationUrl) {
        this.apiId = apiId;
        this.gatewayUrl = gatewayUrl;
        this.version = version;
        this.swaggerUrl = swaggerUrl;
        this.documentationUrl = documentationUrl;
        this.codeSnippet = new ArrayList<>();
    }

    /**
     * apiId - specifies the API identifier that is registered in the API ML installation.
     * <p>
     * The API ID uniquely identifies the API in the API ML.
     * Multiple services can provide the same API. The API ID can be used
     * to locate the same APIs that are provided by different services.
     * The creator of the API defines this ID.
     * The API ID needs to be a string of up to 64 characters
     * that uses lowercase alphanumeric characters and a dot: `.` .
     * <p>
     * We recommend that you use your organization as the prefix.
     * <p>
     * XML Path: /instance/metadata/apiml.apiInfo.${api-index}.apiId
     */
    @JsonProperty(required = true)
    private String apiId;

    private String gatewayUrl;
    private String version;
    private String swaggerUrl;
    private String documentationUrl;

    @Builder.Default
    private List<CodeSnippet> codeSnippet = new ArrayList<>();

    @JsonDeserialize(using = StringToBooleanDeserializer.class)
    @Builder.Default
    private boolean isDefaultApi = false;

    public void addCodeSnippet(CodeSnippet newCodeSnippet) {
        this.codeSnippet.add(newCodeSnippet);
    }

    @JsonIgnore
    public int getMajorVersion() {
        if (version == null) {
            return -1;
        }

        String versionWithoutId = version.replace(apiId + " ", "");
        String[] versionFields = versionWithoutId.split("[^0-9a-zA-Z]");
        String majorVersionStr = versionFields[0].replaceAll("\\D", "");
        return majorVersionStr.isEmpty() ? -1 : Integer.parseInt(majorVersionStr);
    }

    private static class StringToBooleanDeserializer extends JsonDeserializer<Boolean> {

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Boolean.parseBoolean(p.getText());
        }
    }
}
