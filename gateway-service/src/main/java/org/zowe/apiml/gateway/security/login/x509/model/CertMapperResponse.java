/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.x509.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertMapperResponse {
    @JsonProperty("userid")
    private String userId;
    private int rc;
    @JsonProperty("saf_rc")
    private int safRc;
    @JsonProperty("racf_rc")
    private int racfRc;
    @JsonProperty("reason_code")
    private int reasonCode;
}
