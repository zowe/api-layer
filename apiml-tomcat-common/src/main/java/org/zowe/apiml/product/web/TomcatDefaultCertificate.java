/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

@Component
public class TomcatDefaultCertificate implements TomcatConnectorCustomizer {

    private void fixDefaultCertificate(SSLHostConfig sslHostConfig) {
        Set<SSLHostConfigCertificate> originalSet = sslHostConfig.getCertificates();
        if (originalSet.isEmpty()) return;

        try {
            Field defaultCertificateField = sslHostConfig.getClass().getDeclaredField("defaultCertificate");
            defaultCertificateField.setAccessible(true); // NOSONAR
            if (defaultCertificateField.get(sslHostConfig) == null) {
                defaultCertificateField.set(sslHostConfig, originalSet.iterator().next()); // NOSONAR
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot update Tomcat SSL context", e);
        }
    }


    @Override
    public void customize(Connector connector) {
        Arrays.stream(connector.findSslHostConfigs()).forEach(this::fixDefaultCertificate);
    }

}
