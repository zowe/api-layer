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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwkKeys {

    private List<Key> keys;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Key {

        // Cryptographic algorithm family for the certificate's Key pair. i.e. RSA
        @JsonProperty("kty")
        private String kty;

        // The algorithm used with the Key. i.e. RS256
        @JsonProperty("alg")
        private String alg;

        // The certificate's Key ID
        @JsonProperty("kid")
        private String kid;

        // How the Key is used. i.e. sig
        @JsonProperty("use")
        private String use;

        // RSA Key value (exponent) for Key blinding
        @JsonProperty("e")
        private String e;

        // RSA modulus value
        @JsonProperty("n")
        private String n;

    }

}
