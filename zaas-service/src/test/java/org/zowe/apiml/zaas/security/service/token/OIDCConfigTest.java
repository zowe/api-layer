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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class OIDCConfigTest {
    private final ObjectMapper oidcJwkMapper = new OIDCConfig().oidcJwkMapper();

    @Test
    @SneakyThrows
    void shouldParseJwksFormatWithExtraProperties() {
        try (InputStream is = this.getClass().getResourceAsStream("/test_samples/azure_jwks.json")) {
            JwkKeys jwkKeys = oidcJwkMapper.readValue(is, JwkKeys.class);

            assertThat(jwkKeys.getKeys()).hasSize(1);
            JwkKeys.Key key = jwkKeys.getKeys().get(0);
            assertThat(key.getKid()).isEqualTo("9GmnyFPkhc3hOuR22mvSvgnLo7Y");
            assertThat(key.getKty()).isEqualTo("RSA");
        }
    }

    @Test
    @SneakyThrows
    void shouldParseExpectedJwksFormat() {
        try (InputStream is = this.getClass().getResourceAsStream("/test_samples/okta_jwks.json")) {
            JwkKeys jwkKeys = oidcJwkMapper.readValue(is, JwkKeys.class);

            assertThat(jwkKeys.getKeys()).hasSize(2);
            JwkKeys.Key key = jwkKeys.getKeys().get(0);
            assertThat(key.getKid()).isEqualTo("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4");
            assertThat(key.getKty()).isEqualTo("RSA");
        }
    }
}
