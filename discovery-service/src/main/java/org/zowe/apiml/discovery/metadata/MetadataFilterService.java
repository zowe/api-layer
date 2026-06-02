/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.metadata;

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class MetadataFilterService implements InitializingBean {

    @Value("${apiml.security.allowedDomains:${apiml.service.hostname}}")
    private String allowedDomains;

    @InjectApimlLogger
    private ApimlLogger apimlLogger = ApimlLogger.empty();

    private List<String> allowedDomainsList;

    @Override
    public void afterPropertiesSet() throws Exception {
        allowedDomainsList = Arrays.stream(allowedDomains.split(",")).map(String::trim).collect(toList());
    }

    public boolean isAllowedDomain(String domain) {
        if (StringUtils.isBlank(domain)) {
            return true;
        }
        return allowedDomainsList.stream().anyMatch(allowedDomain -> {
            try {
                return isAllowed(allowedDomain, domain);
            } catch (MalformedURLException e) {
                return false;
            }
        });
    }

    private boolean isAllowed(String domain, String value) throws MalformedURLException {
        log.error("checking URL {} against domain {}", value, domain);
        if (isUrl(value)) {
            value = new URL(value).getHost();
        }
        if (value.equals(domain)) {
            return true;
        }
        if (value.startsWith("*.")) {
            return domain.endsWith(value.substring(2));
        }

        return false;
    }

    public void verifyAllowedDomains(InstanceInfo info) throws MetadataValidationException {
        var builder = new StringBuilder();
        if (!isAllowedDomain(info.getHomePageUrl())) {
            builder.append("Home page URL is not allowed: ").append(info.getHomePageUrl()).append(System.lineSeparator());
        }
        if (!isAllowedDomain(info.getHealthCheckUrl())) {
            builder.append("Health check URL is not allowed: ").append(info.getHealthCheckUrl()).append(System.lineSeparator());
        }
        if (!isAllowedDomain(info.getStatusPageUrl())) {
            builder.append("Status page URL is not allowed: ").append(info.getStatusPageUrl()).append(System.lineSeparator());
        }
        if (!isAllowedDomain(info.getSecureHealthCheckUrl())) {
            builder.append("Secure health check URL is not allowed: ").append(info.getSecureHealthCheckUrl()).append(System.lineSeparator());
        }

        info.getMetadata().forEach((key, value) -> {

            if (isUrl(value)) {
                if (!isAllowedDomain(value)) {
                    builder.append("URL ").append(value).append(" in metadata entry ").append(key).append(" is not allowed for instance ").append(info.getInstanceId()).append(System.lineSeparator());
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("URL {} is allowed", value);
                    }
                }
            }
        });

        if (builder.length() > 0) {
            log.warn(builder.toString());
            throw new MetadataValidationException(builder.toString());
        }

    }

    private boolean isUrl(String value) {
        try {
            new URL(value);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }

    }

}
