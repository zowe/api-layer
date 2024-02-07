package org.zowe.apiml.product.web;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryRestTemplateConfigTest {

    @Test
    void giveSecureEurekaUrl_thenCreateSSLConfig() throws NoSuchAlgorithmException {

        var dct = new DiscoveryRestTemplateConfig();
        var args = dct.defaultArgs("https://localhost:10011", SSLContext.getDefault(), new NoopHostnameVerifier());
        assertTrue(args.getSSLContext().isPresent());
    }

    @Test
    void giveUnsecureEurekaUrl_thenDontCreateSSLConfig() throws NoSuchAlgorithmException {

        var dct = new DiscoveryRestTemplateConfig();
        var args = dct.defaultArgs("http://localhost:10011", SSLContext.getDefault(), new NoopHostnameVerifier());
        assertFalse(args.getSSLContext().isPresent());
    }
}
