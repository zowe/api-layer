/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry.interceptor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.discovery.metadata.MetadataTranslationService;
import org.zowe.apiml.product.eureka.web.MetadataFilterService;
import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;

/**
 * The three registration interceptors carry APIML's own registration policy - metadata translation, conformance
 * warnings and the domain allow list - and had no unit tests. They ran as subclass overrides of Netflix's
 * register() under Eureka, so this is the first time the policy is testable in isolation. */
class RegistrationInterceptorsTest {

    private static ServiceInstance instance(String instanceId, String appName, String ipAddr,
                                           Map<String, String> metadata) {
        return ServiceInstance.builder()
            .instanceId(instanceId)
            .appName(appName)
            .hostName("localhost")
            .ipAddr(ipAddr)
            .port(new PortInfo(10010, false))
            .securePort(new PortInfo(10010, true))
            .homePageUrl("https://localhost:10010/" + appName.toLowerCase())
            .status(InstanceStatus.UP)
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .metadata(metadata)
            .build();
    }

    private static ServiceInstance instance(String instanceId, String appName) {
        return instance(instanceId, appName, "127.0.0.1", new LinkedHashMap<>());
    }

    @Nested
    class WhenTranslatingMetadata {

        @Test
        void orderRunsBeforeTheOtherTwo() {
            MetadataTranslationInterceptor interceptor = new MetadataTranslationInterceptor(
                mock(MetadataTranslationService.class), mock(MetadataDefaultsService.class));
            assertEquals(10, interceptor.order());
            assertTrue(interceptor.order() < new ConformanceWarningInterceptor().order());
            assertTrue(new ConformanceWarningInterceptor().order()
                < new DomainAllowListInterceptor(mock(MetadataFilterService.class)).order());
        }

        @Test
        void translatedMetadataIsStoredAndTheOriginalIsNotMutated() {
            MetadataTranslationService translation = mock(MetadataTranslationService.class);
            MetadataDefaultsService defaults = mock(MetadataDefaultsService.class);
            Map<String, String> original = new LinkedHashMap<>();
            original.put("apiml.service.title", "Original");

            // The translation service mutates the map it is handed, as the real one does.
            doAnswer(invocation -> {
                Map<String, String> target = invocation.getArgument(1);
                target.put("apiml.service.title", "Translated");
                return null;
            }).when(translation).translateMetadata(eq("service"), anyMap());

            MetadataTranslationInterceptor interceptor =
                new MetadataTranslationInterceptor(translation, defaults);

            ServiceInstance result = interceptor.intercept(
                instance("localhost:service:10010", "SERVICE", "127.0.0.1", original));

            assertEquals("Translated", result.metadata().get("apiml.service.title"));
            assertEquals("Original", original.get("apiml.service.title"),
                "the incoming instance must not be mutated - it may still be in use elsewhere");
            assertNotSame(original, result.metadata());
            verify(defaults).updateMetadata(eq("service"), anyMap());
        }

        @Test
        void serviceIdIsDerivedFromTheInstanceId() {
            MetadataTranslationService translation = mock(MetadataTranslationService.class);
            MetadataDefaultsService defaults = mock(MetadataDefaultsService.class);

            new MetadataTranslationInterceptor(translation, defaults)
                .intercept(instance("localhost:apicatalog:10014", "APICATALOG"));

            verify(translation).translateMetadata(eq("apicatalog"), anyMap());
            verify(defaults).updateMetadata(eq("apicatalog"), anyMap());
        }

    }

    @Nested
    class WhenWarningAboutConformance {

        @Test
        void itNeverRejectsEvenWhenTheIdentityIsInvalid() {
            ConformanceWarningInterceptor interceptor = new ConformanceWarningInterceptor();

            // A serviceId that cannot be conformant, and an appName that disagrees with it.
            ServiceInstance input = instance("localhost:in_valid:10010", "SOMETHING_ELSE");
            ServiceInstance result = interceptor.intercept(input);

            assertSame(input, result, "conformance problems are warnings, not rejections");
        }

        @Test
        void itWarnsOnAMissingPassTicketApplidButStillRegisters() {
            ConformanceWarningInterceptor interceptor = new ConformanceWarningInterceptor();
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put(AUTHENTICATION_SCHEME, "httpBasicPassTicket");
            // applid deliberately absent

            ServiceInstance input = instance("localhost:service:10010", "SERVICE", "127.0.0.1", metadata);

            assertSame(input, interceptor.intercept(input));
        }

        @Test
        void orderIsBetweenTranslationAndTheAllowList() {
            assertEquals(20, new ConformanceWarningInterceptor().order());
        }

    }

    @Nested
    class WhenApplyingTheDomainAllowList {

        @Test
        void aDisallowedIpAddressIsCorrectedRatherThanRejected() {
            MetadataFilterService filter = mock(MetadataFilterService.class);
            // Behind NAT a service reports an address the registry cannot verify; the resolved one is used.
            when(filter.verifyAllowedDomains(any())).thenReturn("10.0.0.7");

            ServiceInstance result = new DomainAllowListInterceptor(filter)
                .intercept(instance("localhost:service:10010", "SERVICE", "192.168.1.5", new LinkedHashMap<>()));

            assertEquals("10.0.0.7", result.ipAddr());
        }

        @Test
        void anAllowedInstanceIsPassedThroughUnchanged() {
            MetadataFilterService filter = mock(MetadataFilterService.class);
            when(filter.verifyAllowedDomains(any())).thenReturn(null);

            ServiceInstance input = instance("localhost:service:10010", "SERVICE");
            assertSame(input, new DomainAllowListInterceptor(filter).intercept(input));
        }

        @Test
        void aNullResolutionLeavesTheAddressAlone() {
            MetadataFilterService filter = mock(MetadataFilterService.class);
            when(filter.verifyAllowedDomains(any())).thenReturn(null);

            ServiceInstance input = instance("localhost:service:10010", "SERVICE", "192.168.1.5", new LinkedHashMap<>());
            assertEquals("192.168.1.5", new DomainAllowListInterceptor(filter).intercept(input).ipAddr());
        }

        @Test
        void theFilterSeesTheFullInstanceNotJustTheIdentifier() {
            MetadataFilterService filter = mock(MetadataFilterService.class);
            when(filter.verifyAllowedDomains(any())).thenReturn(null);

            new DomainAllowListInterceptor(filter)
                .intercept(instance("localhost:service:10010", "SERVICE"));

            verify(filter).verifyAllowedDomains(any(MetadataFilterService.Candidate.class));
        }

        @Test
        void orderIsLast() {
            assertEquals(30, new DomainAllowListInterceptor(mock(MetadataFilterService.class)).order());
        }

    }

}
