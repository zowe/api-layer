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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Represents one code snippet provided by a service
 */
@NoArgsConstructor
@Data
@SuperBuilder
public class CodeSnippet {

    @JsonProperty(required = true)
    private String endpoint;
    private String codeBlock;
    private String language;

    public CodeSnippet(String endpoint, String codeBlock, String language) {
        this.endpoint = endpoint;
        this.codeBlock = codeBlock;
        this.language = language;
    }
}
