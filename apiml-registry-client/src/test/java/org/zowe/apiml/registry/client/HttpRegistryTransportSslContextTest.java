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

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class HttpRegistryTransportSslContextTest {

    @Test
    void givenCertificatesAreVerified_whenTheContextIsChosen_thenTheSuppliedOneIsKept() throws Exception {
        var supplied = SSLContext.getInstance("TLS");

        assertThat(HttpRegistryTransport.effectiveSslContext(true, supplied)).isSameAs(supplied);
    }

    /**
     * The integration job that runs with {@code apiml.security.ssl.verifySslCertificatesOfServices=false} lost every
     * registration from this client, because switching the hostname verifier off left the trust manager checking the
     * chain: the Discovery Service's certificate was not in the truststore, the handshake failed, and the failure
     * read like a network problem.
     * <p>
     * A handshake cannot be completed in a unit test, but the choice of context can be, and the choice was the bug.
     */
    @Test
    void givenCertificatesAreNotVerified_whenTheContextIsChosen_thenTheChainIsNotChecked() throws Exception {
        var supplied = SSLContext.getInstance("TLS");

        var chosen = HttpRegistryTransport.effectiveSslContext(false, supplied);

        assertThat(chosen).isNotSameAs(supplied);

        // the context will not hand its trust managers back, so the manager itself is asserted
        var anyCertificate = mock(X509Certificate.class);
        var trustManager = HttpRegistryTransport.trustAllTrustManager();
        assertThatCode(() -> trustManager.checkServerTrusted(new X509Certificate[]{anyCertificate}, "RSA"))
            .doesNotThrowAnyException();
        assertThat(trustManager.getAcceptedIssuers()).isEmpty();
    }
}
