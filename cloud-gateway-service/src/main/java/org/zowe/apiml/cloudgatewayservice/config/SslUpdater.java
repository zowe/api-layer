/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Ssl;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.SecurityUtils;

@Component
public class SslUpdater implements BeanPostProcessor {

    private static final String KEYRING_PASSWORD = "password";

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ServerProperties) {
            ServerProperties serverProperties = (ServerProperties) bean;
            Ssl ssl = serverProperties.getSsl();
            if (SecurityUtils.isKeyring(ssl.getKeyStore())) {
                ssl.setKeyStore(SecurityUtils.formatKeyringUrl(ssl.getKeyStore()));
                if (ssl.getKeyStorePassword() == null) {
                    ssl.setKeyStorePassword(KEYRING_PASSWORD);
                }
            }
            if (SecurityUtils.isKeyring(ssl.getTrustStore())) {
                ssl.setTrustStore(SecurityUtils.formatKeyringUrl(ssl.getTrustStore()));
                if (ssl.getTrustStorePassword() == null) {
                    ssl.setTrustStorePassword(KEYRING_PASSWORD);
                }
            }
        }
        return bean;
    }

}
