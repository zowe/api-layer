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
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.zowe.apiml.security.HttpsConfig;

import java.io.File;
import java.io.IOException;

@Slf4j
public class TomcatServerFactory {
    private static final String SERVLET_NAME = "hello";

    public Tomcat startTomcat(HttpsConfig httpsConfig) throws IOException {
        Tomcat tomcat = new Tomcat();
        String contextPath = new File(".").getCanonicalPath();
        log.info("Tomcat contextPath: {}", contextPath);
        Context ctx = tomcat.addContext("", contextPath);
        tomcat.setConnector(createHttpsConnector(httpsConfig));


        Tomcat.addServlet(ctx, SERVLET_NAME, new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.setCharacterEncoding("UTF-8");
                resp.setContentType("text/plain");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("OK");
                resp.getWriter().flush();
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

        var sslHostConfig = new SSLHostConfig();
        httpsConnector.addSslHostConfig(sslHostConfig);

        SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.RSA);
        certificate.setCertificateKeystoreFile(httpsConfig.getKeyStore());
        certificate.setCertificateKeystorePassword(String.valueOf(httpsConfig.getKeyStorePassword()));
        certificate.setCertificateKeyAlias(httpsConfig.getKeyAlias());

        sslHostConfig.addCertificate(certificate);
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
}
