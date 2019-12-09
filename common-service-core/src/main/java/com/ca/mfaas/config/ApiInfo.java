/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents one API provided by a service
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ApiInfo {

    /**
     * apiId - specifies the API identifier that is registered in the API ML installation.

     * The API ID uniquely identifies the API in the API ML.
     * Multiple services can provide the same API. The API ID can be used
     * to locate the same APIs that are provided by different services.
     * The creator of the API defines this ID.
     * The API ID needs to be a string of up to 64 characters
     * that uses lowercase alphanumeric characters and a dot: `.` .
     *
     * We recommend that you use your organization as the prefix.
     *
     * XML Path: /instance/metadata/apiml.apiInfo.${api-index}.apiId
     */
    @JsonProperty(required = true)
    private String apiId;


    private String gatewayUrl;
    private String version;
    private String swaggerUrl;
    private String documentationUrl;
}
