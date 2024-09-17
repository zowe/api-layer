/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config.oidc;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Reads OIDC Client configuration from environment variables or application configuration file.
 */
@Data
@Component
@Slf4j
@ConfigurationProperties(prefix = "spring.security.oauth2.client", ignoreInvalidFields = true)
public class ClientConfiguration {

    private static final String SYSTEM_ENV_PREFIX = "ZWE_configs_spring_security_oauth2_client_";
    private static final Pattern REGISTRATION_ID_PATTERN = Pattern.compile(
        "^" + SYSTEM_ENV_PREFIX + "(registration|provider)_([^_]+)_.*$"
    );

    public static final String REGISTRATION_ENV_TYPE = "registration";
    public static final String PROVIDER_ENV_TYPE = "provider";


    private Map<String, Registration> registration = new HashMap<>();
    private Map<String, Provider> provider = new HashMap<>();

    private String getSystemEnv(String id, String type, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_ENV_PREFIX).append(type).append('_').append(id).append('_').append(name);
        return System.getenv(sb.toString());
    }

    private void update(String id, String type, String base, Consumer<String> setter) {
        String systemEnv = getSystemEnv(id, type, base);
        if (systemEnv != null) {
            setter.accept(systemEnv);
        }
    }

    private void update(String id, Registration registration) {
        update(id, REGISTRATION_ENV_TYPE, "clientId", registration::setClientId);
        update(id, REGISTRATION_ENV_TYPE, "clientSecret", registration::setClientSecret);
        update(id, REGISTRATION_ENV_TYPE, "redirectUri", registration::setRedirectUri);

        String scope = getSystemEnv(id, REGISTRATION_ENV_TYPE, "scope");
        if (scope != null) {
            registration.setScope(Arrays.asList(scope.split("[,]")));
        }
    }

    private void update(String id, Provider provider) {
        update(id, PROVIDER_ENV_TYPE, "authorizationUri", provider::setAuthorizationUri);
        update(id, PROVIDER_ENV_TYPE, "tokenUri", provider::setTokenUri);
        update(id, PROVIDER_ENV_TYPE, "userInfoUri", provider::setUserInfoUri);
        update(id, PROVIDER_ENV_TYPE, "userNameAttribute", provider::setUserNameAttribute);
        update(id, PROVIDER_ENV_TYPE, "jwkSetUri", provider::setJwkSetUri);
    }

    private Set<String> getRegistrationsIdsFromSystemEnv() {
        return System.getenv().keySet().stream()
            .map(key -> {
                Matcher matcher = REGISTRATION_ID_PATTERN.matcher(String.valueOf(key));
                if (matcher.matches()) {
                    return matcher.group(2);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    @PostConstruct
    void updateWithSystemEnvironment() {
        for (String registrationId : getRegistrationsIdsFromSystemEnv()) {
            update(registrationId, registration.computeIfAbsent(registrationId, k -> new Registration()));
            update(registrationId, provider.computeIfAbsent(registrationId, k -> new Provider()));
        }
    }

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
        if (map.size() < Math.max(registration.size(), provider.size())) {
            log.debug("OIDC configuration is not complete, please refer to the documentation.");
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
