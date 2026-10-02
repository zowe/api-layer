/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang.ArrayUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ssl {
    private Boolean enabled = true;

    private Boolean verifySslCertificatesOfServices = true;

    private Boolean nonStrictVerifySslCertificatesOfServices = false;

    private String protocol;

    private String keyAlias;

    @ToString.Exclude
    private char[] keyPassword;

    private String keyStore;

    @ToString.Exclude
    private char[] keyStorePassword;

    private String keyStoreType;

    private String trustStore;

    @ToString.Exclude
    private char[] trustStorePassword;

    private String trustStoreType;

    @ToString.Include(name = "keyPassword")
    private String maskedKeyPassword() {
        return masked(keyPassword);
    }

    @ToString.Include(name = "keyStorePassword")
    private String maskedKeyStorePassword() {
        return masked(keyStorePassword);
    }

    @ToString.Include(name = "trustStorePassword")
    private String maskedTrustStorePassword() {
        return masked(trustStorePassword);
    }

    private String masked(char[] password) {
        if (ArrayUtils.isEmpty(password)) {
            return "null";
        }
        return "*****";
    }

}
