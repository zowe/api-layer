/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Information about expected authentication scheme and APPLID for PassTickets generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Authentication {

    private AuthenticationScheme scheme;
    private String applid;
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private Boolean supportsSso;

    public Authentication(AuthenticationScheme scheme, String applid) {
        this.scheme = scheme;
        this.applid = applid;
        this.supportsSso = null;
    }

    @JsonProperty
    public boolean supportsSso() {
        switch (scheme) {
            case ZOWE_JWT:
            case HTTP_BASIC_PASSTICKET:
            case ZOSMF:
                return supportsSso == null || supportsSso;
            case BYPASS:
            default:
                return supportsSso != null && supportsSso;
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return (scheme == null) && (applid == null);
    }

}
