/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config.oidc;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.client", ignoreInvalidFields = true)
public class ClientConfiguration {

    private Map<String, Registration> registration = new HashMap<>();
    private Map<String, Provider> provider = new HashMap<>();

    public Map<String, Config> getConfigurations() {
        Map<String, Config> map = new HashMap<>();
        for (Map.Entry<String, Registration> registrationEntry : registration.entrySet()) {
            String id = registrationEntry.getKey();
            Provider providerConfig = provider.get(id);
            if (providerConfig != null) {
                map.put(id, Config.builder()
                    .id(id)
                    .registration(registrationEntry.getValue())
                    .provider(providerConfig)
                    .build()
                );
            }
        }
        return map;
    }

    public boolean isConfigured() {
        if (!Optional.ofNullable(registration).map(m -> !m.isEmpty()).orElse(false)) {
            return false;
        }
        return Optional.ofNullable(provider).map(m -> !m.isEmpty()).orElse(false);
    }

    @Value
    @Builder(access = AccessLevel.PACKAGE)
    public static class Config {

        private String id;
        private Registration registration;
        private Provider provider;

    }

}
