/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.codec.RegistryCodec;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code EurekaBasicAuthEnvironmentPostProcessor} injects credentials into
 * {@code eureka.client.serviceUrl.defaultZone}. The Eureka client tolerated that; Apache HttpClient 5 does not, and
 * refusing the URL cost every registration and renewal in the jobs that set {@code apiml.discovery.userid} and
 * {@code apiml.discovery.password}.
 */
class HttpRegistryTransportCredentialsTest {

    @Test
    void givenCredentialsInTheUrl_whenTheUrlIsCleaned_thenTheyAreTakenOutAndKept() {
        var address = HttpRegistryTransport.withoutEmbeddedCredentials(
            "https://eureka:password@discovery-service:10011/eureka/");

        assertThat(address.url()).isEqualTo("https://discovery-service:10011/eureka/");
        assertThat(address.userid()).isEqualTo("eureka");
        assertThat(address.password()).isEqualTo("password");
    }

    @Test
    void givenNoCredentialsInTheUrl_whenTheUrlIsCleaned_thenItIsUntouched() {
        var address = HttpRegistryTransport.withoutEmbeddedCredentials(
            "https://discovery-service:10011/eureka/");

        assertThat(address.url()).isEqualTo("https://discovery-service:10011/eureka/");
        assertThat(address.userid()).isNull();
        assertThat(address.password()).isNull();
    }

    @Test
    void givenAnAtSignOutsideTheAuthority_whenTheUrlIsCleaned_thenItIsNotTakenForCredentials() {
        var address = HttpRegistryTransport.withoutEmbeddedCredentials(
            "https://discovery-service:10011/eureka/@self");

        assertThat(address.url()).isEqualTo("https://discovery-service:10011/eureka/@self");
        assertThat(address.userid()).isNull();
    }

    @Test
    void givenPercentEncodedCredentials_whenTheUrlIsCleaned_thenTheyAreDecoded() {
        var address = HttpRegistryTransport.withoutEmbeddedCredentials(
            "https://user%40name:p%40ss@discovery-service:10011/eureka/");

        assertThat(address.userid()).isEqualTo("user@name");
        assertThat(address.password()).isEqualTo("p@ss");
    }

    @Test
    void givenCredentialsInTheUrl_whenTheTransportIsBuilt_thenTheyBecomeTheAuthenticationHeader() throws Exception {
        var sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);

        try (var transport = new HttpRegistryTransport(
            List.of("https://eureka:password@discovery-service:10011/eureka/"),
            sslContext, new RegistryCodec(), true, 1000, 1000, "someone-else", "secret")) {

            assertThat(transport.serviceUrls()).containsExactly("https://discovery-service:10011/eureka/");
            assertThat(transport.basicAuthHeader()).isEqualTo("Basic " + Base64.getEncoder()
                .encodeToString("eureka:password".getBytes(StandardCharsets.UTF_8)));
        }
    }
}
