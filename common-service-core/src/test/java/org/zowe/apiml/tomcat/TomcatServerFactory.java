/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.tomcat;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.zowe.apiml.security.HttpsConfig;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

@Slf4j
public class TomcatServerFactory {
    private static final String SERVLET_NAME = "hello";
    private static final char[] STORE_PASSWORD = "password".toCharArray();  // NOSONAR

    public Tomcat startTomcat(HttpsConfig httpsConfig) throws IOException {
        Tomcat tomcat = new Tomcat();
        String contextPath = new File(".").getCanonicalPath();
        log.info("Tomcat contextPath: {}", contextPath);
        Context ctx = tomcat.addContext("", contextPath);
        tomcat.setConnector(createHttpsConnector(httpsConfig));
        Tomcat.addServlet(ctx, SERVLET_NAME, new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse response) throws ServletException, IOException {
                response.setCharacterEncoding("UTF-8");
                response.setContentType("text/plain");
                try (Writer writer = response.getWriter()) {
                    writer.write("OK");
                    writer.flush();
                }
            }
        });

        ctx.addServletMappingDecoded("/*", SERVLET_NAME);
        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);  // NOSONAR
        }
        return tomcat;
    }


    private Connector createHttpsConnector(HttpsConfig httpsConfig) {
        Connector httpsConnector = new Connector();
        httpsConnector.setPort(0);
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setProperty("clientAuth",
            Boolean.toString(Boolean.parseBoolean(httpsConfig.getClientAuth()) && httpsConfig.isVerifySslCertificatesOfServices()));
        httpsConnector.setProperty("keystoreFile", httpsConfig.getKeyStore());
        httpsConnector.setProperty("ciphers", String.join(",", httpsConfig.getCipherSuite()));
        httpsConnector.setProperty("enabled-protocols", String.join("+", httpsConfig.getEnabledProtocols()));
        httpsConnector.setProperty("keystorePass",
            httpsConfig.getKeyStorePassword() == null ? null : String.valueOf(httpsConfig.getKeyStorePassword())
        );
        if (Boolean.parseBoolean(httpsConfig.getClientAuth()) || "want".equals(httpsConfig.getClientAuth())) {
            httpsConnector.setProperty("truststoreFile", httpsConfig.getTrustStore());
            httpsConnector.setProperty("truststorePass",
                httpsConfig.getTrustStorePassword() == null ? null : String.valueOf(httpsConfig.getTrustStorePassword())
            );
        }
        httpsConnector.setProperty("sslProtocol", httpsConfig.getProtocol());
        httpsConnector.setProperty("SSLEnabled", "true");
        return httpsConnector;
    }

    static int getLocalPort(Tomcat tomcat) {
        Service[] services = tomcat.getServer().findServices();
        for (Service service : services) {
            for (Connector connector : service.findConnectors()) {
                ProtocolHandler protocolHandler = connector.getProtocolHandler();
                if (protocolHandler instanceof Http11NioProtocol) {
                    return connector.getLocalPort();
                }
            }
        }
        return 0;
    }

//    public static void main(String[] args) throws LifecycleException, ClientProtocolException, IOException {
//        log.debug("Cwd: {}", System.getProperty("user.dir"));
//
//        HttpsConfig httpsConfig = HttpsConfig.builder()
//            .keyStore(new File("keystore/localhost/localhost.keystore.p12").getCanonicalPath())
//            .keyStorePassword(STORE_PASSWORD).keyPassword(STORE_PASSWORD)
//            .trustStore(new File("keystore/localhost/localhost.truststore.p12").getCanonicalPath())
//            .trustStorePassword(STORE_PASSWORD).protocol("TLSv1.2").build();
//        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
//
//        Tomcat tomcat = new TomcatServerFactory().startTomcat(httpsConfig);
//        try {
//
//            HttpClient client = httpsFactory.createSecureHttpClient(null);
//
//            int port = getLocalPort(tomcat);
//            HttpGet get = new HttpGet(String.format("https://localhost:%d", port));
//            HttpResponse response = client.execute(get);
//
//            String responseBody = EntityUtils.toString(response.getEntity());
//
//            assertEquals(200, response.getStatusLine().getStatusCode());
//            assertEquals("OK", responseBody);
//        } finally {
//            tomcat.stop();
//        }
//    }
}
