/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.token;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ZaasToken {
    String domain;
    String userId;
    Date creation;
    Date expiration;
    boolean expired;

    @Override
    public String toString() {
        return "ZaasToken{" +
            "domain='" + domain + '\'' +
            ", userId='" + userId + '\'' +
            ", creation=" + creation +
            ", expiration=" + expiration +
            ", expired=" + expired +
            '}';
    }
}
