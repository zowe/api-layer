/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.zosmf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.ServiceCacheEvict;
import org.zowe.apiml.gateway.security.service.ZosmfService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * This bean is default implementation of access to z/OSMF authentication. It collect all other implementation and
 * select right implementation by z/OSMF version. This bean is facade for those implementation and all its own methods
 * are delegates to implementation based of z/OSMF version.
 *
 * see also:
 *  - {@link ZosmfServiceV2}
 *      - new version supporting https://www.ibm.com/support/knowledgecenter/SSLTBW_2.4.0/com.ibm.zos.v2r4.izua700/izuprog_API_WebTokenAuthServices.htm
 *  - {@link ZosmfServiceV1}
 *      - old version using endpoint /zosmf/info
 */
@Primary
@Service
public class ZosmfServiceFacade extends AbstractZosmfService implements ServiceCacheEvict {

    protected final ApplicationContext applicationContext;
    protected final List<ZosmfService> implementations;

    private ZosmfServiceFacade meProxy;

    public ZosmfServiceFacade(
        final AuthConfigurationProperties authConfigurationProperties,
        final DiscoveryClient discovery,
        final @Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore,
        final ObjectMapper securityObjectMapper,
        final ApplicationContext applicationContext,
        final List<ZosmfService> implementations
    ) {
        super(
            authConfigurationProperties,
            discovery,
            restTemplateWithoutKeystore,
            securityObjectMapper
        );
        this.applicationContext = applicationContext;
        this.implementations = implementations;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        meProxy = applicationContext.getBean(ZosmfServiceFacade.class);
    }

    @CacheEvict(value = {"zosmfVersion", "zosmfServiceImplementation"}, allEntries = true)
    public void evictCaches() {
        // evict all caches
    }

    @Cacheable("zosmfVersion")
    public int getVersion(String zosmfServiceId) {
        final String url = getURI(zosmfServiceId) + ZOSMF_INFO_END_POINT;
        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");

        try {
            final ResponseEntity<ZosmfInfo> info = restTemplateWithoutKeystore.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), ZosmfInfo.class
            );
            final ZosmfInfo body = info.getBody();
            if (body == null) return 0;
            return body.getVersion();
        } catch (RuntimeException re) {
            meProxy.evictCaches();
            throw handleExceptionOnCall(url, re);
        }
    }

    @Cacheable("zosmfServiceImplementation")
    public ZosmfService getImplementation(String zosmfServiceId) {
        final int version = meProxy.getVersion(zosmfServiceId);
        for (final ZosmfService zosmfService : implementations) {
            if (zosmfService.matchesVersion(version)) return zosmfService;
        }

        meProxy.evictCaches();
        throw new IllegalArgumentException("Unknown version of z/OSMF : " + version);
    }

    protected ZosmfService getImplementation() {
        return meProxy.getImplementation(getZosmfServiceId());
    }

    @Override
    public AuthenticationResponse authenticate(Authentication authentication) {
        return getImplementation().authenticate(authentication);
    }

    @Override
    public void validate(TokenType type, String token) {
        getImplementation().validate(type, token);
    }

    @Override
    public void invalidate(TokenType type, String token) {
        getImplementation().invalidate(type, token);
    }

    @Override
    public boolean matchesVersion(int version) {
        return false;
    }

    @Override
    public void evictCacheAllService() {
        meProxy.evictCaches();
    }

    @Override
    public void evictCacheService(String serviceId) {
        if (StringUtils.equalsIgnoreCase(getZosmfServiceId(), serviceId)) {
            meProxy.evictCaches();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZosmfInfo {

        @JsonProperty("zosmf_version")
        private int version;

        @JsonProperty("zosmf_full_version")
        private String fullVersion;

    }

}
