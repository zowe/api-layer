/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCTokenClaims {
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("scope")
    private String scopes;
    @JsonProperty("username")
    private String username;
    @JsonProperty("exp")
    private Long expiresAt;
    @JsonProperty("iat")
    private Long issuedAt;
    @JsonProperty("sub")
    private String subject;
    @JsonProperty("aud")
    private String audience;
    @JsonProperty("iss")
    private String issuer;
    @JsonProperty("jti")
    private String jwtId;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("uid")
    private String uid;
}
