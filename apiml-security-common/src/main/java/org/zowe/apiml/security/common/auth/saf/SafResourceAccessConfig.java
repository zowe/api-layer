/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;

@Configuration
@Slf4j
public class SafResourceAccessConfig {

    private static final String ENDPOINT = "endpoint";
    private static final String NATIVE = "native";
    private static final String DUMMY = "dummy";

    private static final String[] PROVIDERS = new String[] { ENDPOINT, NATIVE, DUMMY };

    @Value("${apiml.security.authorization.provider:}")
    private String provider;

    @Value("${apiml.security.authorization.endpoint.enabled:false}")
    private boolean endpointEnabled;

    protected SafResourceAccessVerifying createEndpoint(RestTemplate restTemplate) {
        return new SafResourceAccessEndpoint(restTemplate);
    }

    protected SafResourceAccessVerifying createNative() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        return new SafResourceAccessSaf();
    }

    protected SafResourceAccessVerifying createDummy() throws IOException {
        return new SafResourceAccessDummy();
    }

    private SafResourceAccessVerifying create(RestTemplate restTemplate, String type, boolean force) {
        switch (StringUtils.lowerCase(type)) {
            case ENDPOINT:
                if (endpointEnabled || force) {
                    return createEndpoint(restTemplate);
                }
                return null;
            case NATIVE:
                try {
                    return createNative();
                } catch (Exception e) {
                    log.debug("API PlatformAccessControl is not available", e);
                }
                return null;
            case DUMMY:
                try {
                    return createDummy();
                } catch (Exception e) {
                    log.debug("saf.yml nor mock-saf.yml is not available", e);
                }
                return null;
            default:
                throw new IllegalArgumentException(
                    String.format("Unknown provider to check SAF resource '%s', use one from %s",
                       type, Arrays.toString(PROVIDERS)
                    ));
        }
    }

    @Bean
    public SafResourceAccessVerifying safResourceAccessVerifying(RestTemplate restTemplate) {
        if (!StringUtils.isEmpty(provider)) {
            return create(restTemplate, provider, true);
        }

        for (String type : PROVIDERS) {
            SafResourceAccessVerifying srv = create(restTemplate, type, false);
            if (srv != null) return srv;
        }

        return null;
    }

}
