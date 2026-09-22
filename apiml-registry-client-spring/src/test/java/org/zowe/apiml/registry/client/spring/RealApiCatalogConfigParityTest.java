/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The API Catalog's real configuration, as opposed to the {@code catalog-instance.yml} fixture.
 * <p>
 * {@code api-catalog-services/src/main/resources/application.yml} sets
 * {@code eureka.instance.statusPageUrlPath} to a complete URL:
 * <pre>
 * statusPageUrlPath: ${apiml.service.scheme}://${apiml.service.hostname}:${apiml.service.port}/apicatalog/application/info
 * </pre>
 * The fixture used by {@link RegistryInstancePropertiesContractTest} puts a relative path
 * ({@code /apicatalog/application/info}) in the same key instead, which is not what any real API Catalog sends.
 * That difference is what its green result rests on, so this test binds the value the service actually ships.
 * <p>
 * It also records a deliberate divergence from Netflix. Netflix prefixes the non-secure host and port onto that
 * value even though it is already a complete URL, producing
 * {@code http://localhost:80https://localhost:10014/apicatalog/application/info}. That string reaches the
 * Discovery Service, where the domain allow list rejects it: {@code MetadataFilterService} logs ZWEAM601W and
 * then throws {@code DomainAllowListMetadataException} because {@code ZWE_ONLY_WARN_ON_URL_NOT_ALLOWED} is unset,
 * and {@code DomainAllowListInterceptor} does not catch it. The registration is therefore refused outright and
 * the API Catalog never appears in {@code /eureka/apps}, which is why the startup check times out waiting for
 * {@code localhost:apicatalog:10014}.
 * <p>
 * Reproducing that faithfully is not parity, it is reproducing a defect, so the replacement client keeps the
 * configured URL as it stands.
 */
class RealApiCatalogConfigParityTest {

    private static final String CONFIGURED_STATUS_PAGE_URL =
        "https://localhost:10014/apicatalog/application/info";

    private static Binder binderFor(String resource) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
            .load(resource, new ClassPathResource(resource));
        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        return Binder.get(environment);
    }

    private static EurekaInstanceConfigBean netflix(String resource) throws IOException {
        EurekaInstanceConfigBean bean = new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties()));
        binderFor(resource).bind("eureka.instance", Bindable.ofInstance(bean));
        bean.setAppname("apicatalog");
        bean.setVirtualHostName("apicatalog");
        bean.setSecureVirtualHostName("apicatalog");
        return bean;
    }

    private static RegistryInstanceProperties ours(String resource) throws IOException {
        RegistryInstanceProperties properties = new RegistryInstanceProperties();
        binderFor(resource).bind("eureka.instance", Bindable.ofInstance(properties));
        RegistryInstanceDefaults.apply(properties, "apicatalog", new InetUtils(new InetUtilsProperties()));
        return properties;
    }

    @Test
    @DisplayName("the status page URL stays the complete URL the API Catalog configured")
    void statusPageUrlKeepsTheConfiguredAbsoluteUrl() throws IOException {
        ServiceInstance actual = SelfInstanceFactory.create(ours("real-catalog-instance.yml"), 1_700_000_000_000L);

        System.out.println("REAL ours.statusPageUrl = " + actual.statusPageUrl());

        assertEquals(CONFIGURED_STATUS_PAGE_URL, actual.statusPageUrl(),
            "a configured URL must not have a host and port prefixed onto it");
        assertFalse(actual.statusPageUrl().startsWith("http://localhost:80https://"),
            "the double-scheme URL is what the domain allow list rejects");
    }

    @Test
    @DisplayName("Netflix builds a URL the registry refuses, which is why this diverges from it")
    void netflixBuildsTheUrlThatBreaksRegistration() throws IOException {
        InstanceInfo expected = new InstanceInfoFactory().create(netflix("real-catalog-instance.yml"));

        System.out.println("REAL netflix.statusPageUrl = " + expected.getStatusPageUrl());

        assertEquals("http://localhost:80" + CONFIGURED_STATUS_PAGE_URL, expected.getStatusPageUrl(),
            "documenting the value Netflix produces for this configuration; the allow list rejects it");
        assertTrue(RegistryInstanceProperties.isAbsoluteUrl(expected.getStatusPageUrl()),
            "the malformed value is still 'absolute' by scheme test, which is why the guard must not rely on it alone");
    }

    @Test
    @DisplayName("a genuine relative path still gets the host and port, as before")
    void relativePathStillGetsThePrefix() throws IOException {
        RegistryInstanceProperties properties = ours("catalog-instance.yml");
        ServiceInstance actual = SelfInstanceFactory.create(properties, 1_700_000_000_000L);

        assertEquals("http://localhost:80/apicatalog/application/info", actual.statusPageUrl(),
            "the relative-path fixture from the existing contract test is unaffected by the guard");
    }

    @Test
    @DisplayName("the home page URL is unchanged: it was configured explicitly all along")
    void homePageUrlParityWithTheRealCatalogConfig() throws IOException {
        InstanceInfo expected = new InstanceInfoFactory().create(netflix("real-catalog-instance.yml"));
        ServiceInstance actual = SelfInstanceFactory.create(ours("real-catalog-instance.yml"), 1_700_000_000_000L);

        assertEquals(expected.getHomePageUrl(), actual.homePageUrl());
        assertEquals("https://localhost:10014/apicatalog", actual.homePageUrl());
    }

}
