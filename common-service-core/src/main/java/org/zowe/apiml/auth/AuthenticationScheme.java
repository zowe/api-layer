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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum AuthenticationScheme {
    @JsonProperty("bypass")
    BYPASS("bypass"),

    @JsonProperty("zoweJwt")
    ZOWE_JWT("zoweJwt"),

    @JsonProperty("httpBasicPassTicket")
    HTTP_BASIC_PASSTICKET("httpBasicPassTicket"),

    @JsonProperty("zosmf")
    ZOSMF("zosmf"),

    @JsonProperty("x509")
    X509("x509"),

    @JsonProperty("safIdt")
    SAF_IDT("safIdt");

    public final String scheme;

    AuthenticationScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public String toString() {
        return scheme;
    }

}
