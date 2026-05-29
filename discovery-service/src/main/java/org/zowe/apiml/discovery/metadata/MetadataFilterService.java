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

    private List<String> allowedDomainsList;

    @Override
    public void afterPropertiesSet() throws Exception {
        allowedDomainsList = Arrays.stream(allowedDomains.split(",")).map(String::trim).collect(toList());
    }

    public boolean isAllowedDomain(String domain) {
        if (StringUtils.isBlank(domain)) {
            return true;
        }
        return allowedDomainsList.stream().anyMatch(allowedDomain -> isAllowed(domain, allowedDomain));
    }

    private boolean isAllowed(String domain, String allowedDomain) {
        if (allowedDomain.equals(domain)) {
            return true;
        }
        if (allowedDomain.startsWith("*.")) {
            return domain.endsWith(allowedDomain.substring(2));
        }

        return false;
    }

    public void verifyAllowedDomains(InstanceInfo info) throws MetadataValidationException {
        var builder = new StringBuilder();
        if (!isAllowedDomain(info.getHomePageUrl())) {
            builder.append("Home page URL is not allowed: ").append(info.getHomePageUrl());
        }
        if (!isAllowedDomain(info.getHealthCheckUrl())) {
            builder.append("Health check URL is not allowed: ").append(info.getHealthCheckUrl());
        }
        if (!isAllowedDomain(info.getStatusPageUrl())) {
            builder.append("Status page URL is not allowed: ").append(info.getStatusPageUrl());
        }
        if (!isAllowedDomain(info.getSecureHealthCheckUrl())) {
            builder.append("Secure health check URL is not allowed: ").append(info.getSecureHealthCheckUrl());
        }

        info.getMetadata().forEach((key, value) -> { // TODO Perhaps it should be only on a set of known metadata entries?
            if (isUrl(value)) {
                if (!isAllowedDomain(value)) {
                    log.warn("URL {} is not allowed for instance {}", value, info.getInstanceId());

                    throw new MetadataValidationException("URL is not allowed: " + value + " for instance " + info.getInstanceId());
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("URL {} is allowed", value);
                    }
                }
            }
        });

        if (builder.length() > 0) {
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
