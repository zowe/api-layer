package org.zowe.apiml;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainTest {

    static HttpsServer httpServer;
    static AssertionError error;

    @BeforeAll
    static void setup() throws Exception {
        var inetAddress = new InetSocketAddress("127.0.0.1", 8080);
        httpServer = HttpsServer.create(inetAddress, 0);

        var sslContext = SSLContext.getInstance("TLS");
        var ks = KeyStore.getInstance("PKCS12");
        try (var fis = new FileInputStream("../keystore/localhost/localhost.keystore.p12")) {
            ks.load(fis, "password".toCharArray());
        }
        var km = KeyManagerFactory.getInstance("SunX509");
        km.init(ks, "password".toCharArray());
        var ts = KeyStore.getInstance("PKCS12");
        try (var fis = new FileInputStream("../keystore/localhost/localhost.truststore.p12")) {
            ts.load(fis, "password".toCharArray());
        }
        var tm = TrustManagerFactory.getInstance("SunX509");
        tm.init(ts);

        sslContext.init(km.getKeyManagers(), tm.getTrustManagers(), null);

        var httpsConfigurator = new TestHttpsConfigurator(sslContext);
        httpServer.setHttpsConfigurator(httpsConfigurator);
        httpServer.createContext("/gateway/api/v1/auth/login", exchange -> {

            exchange.sendResponseHeaders(204, 0);
            var clientCert = ((HttpsExchange) exchange).getSSLSession().getPeerCertificates();
            try {
                // client certificate must be present at this stage
                assertNotNull(clientCert);
            } catch (AssertionError e) {
                error = e;
            }
            exchange.close();
        });
        httpServer.start();

    }

    @AfterAll
    static void tearDown() {

        httpServer.stop(0);
        if (error != null) {
            throw error;
        }
    }

    @Test
    void givenHttpsRequestWithClientCertificate_thenPeerCertificateMustBeAvailable() {
        // Assertion is done on the server to make sure that client certificate was delivered.
        // Assertion error is then rethrown in the tear down method in case certificate was not present.
        Main.main(null);
    }

    static class TestHttpsConfigurator extends HttpsConfigurator {
        /**
         * Creates a Https configuration, with the given {@link SSLContext}.
         *
         * @param context the {@code SSLContext} to use for this configurator
         * @throws NullPointerException if no {@code SSLContext} supplied
         */
        public TestHttpsConfigurator(SSLContext context) {
            super(context);
        }

        @Override
        public void configure(HttpsParameters params) {
            var parms = getSSLContext().getDefaultSSLParameters();
            parms.setNeedClientAuth(true);
            params.setWantClientAuth(true);
            params.setSSLParameters(parms);
        }
    }

}
