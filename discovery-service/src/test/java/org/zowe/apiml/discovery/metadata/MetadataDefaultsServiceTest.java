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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.discovery.registry.interceptor.MetadataTranslationInterceptor;
import org.zowe.apiml.discovery.staticdef.ServiceDefinitionProcessor;
import org.zowe.apiml.discovery.staticdef.StaticServicesRegistrationService;
import org.zowe.apiml.product.discovery.StaticRegistrationResult;
import org.zowe.apiml.registry.InMemoryServiceRegistry;
import org.zowe.apiml.registry.RegistrySettings;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;

/**
 * Service-override metadata is applied to statically-defined services.
 * <p>
 * Rewritten against a real {@link InMemoryServiceRegistry} rather than a mocked one. Under Eureka this behaviour
 * was spread across a post-registration event listener that mutated the stored instance's metadata map, so the
 * test had to stub {@code registerStatically} to fire a fake registration event and seed a static
 * {@code EurekaServerContextHolder}. Metadata translation is now a registration interceptor, applied before the
 * instance is stored, so the test can simply register and read back - and it exercises the real interceptor
 * chain instead of the scaffolding.
 */
class MetadataDefaultsServiceTest {

    private final MetadataTranslationService metadataTranslationService = new MetadataTranslationService();
    private final MetadataDefaultsService metadataDefaultsService = new MetadataDefaultsService();
    private final ServiceDefinitionProcessorMock serviceDefinitionProcessor = new ServiceDefinitionProcessorMock();

    private StaticServicesRegistrationService staticServicesRegistrationService;

    @BeforeEach
    void setUp() {
        var registry = new InMemoryServiceRegistry(
            RegistrySettings.defaults(),
            List.of(new MetadataTranslationInterceptor(metadataTranslationService, metadataDefaultsService)),
            System::currentTimeMillis
        );
        staticServicesRegistrationService = new StaticServicesRegistrationService(
            serviceDefinitionProcessor, metadataDefaultsService, registry);
    }

    @Test
    void updatingStaticService() {
        serviceDefinitionProcessor.setLocation("api-defs");
        ReflectionTestUtils.setField(
            staticServicesRegistrationService, "staticApiDefinitionsDirectories", "api-defs");

        staticServicesRegistrationService.reloadServices();
        Map<String, ServiceInstance> map = staticServicesRegistrationService.getStaticInstances().stream()
            .collect(toMap(ServiceInstance::instanceId, Function.identity()));

        assertEquals(
            "TSTAPPL4",
            map.get("STATIC-localhost:toaddauth:10012").metadata().get(AUTHENTICATION_APPLID)
        );

        assertEquals(
            "TSTAPPL5",
            map.get("STATIC-localhost:toreplaceauth:10012").metadata().get(AUTHENTICATION_APPLID)
        );

        assertEquals(
            "TSTAPPL3",
            map.get("STATIC-localhost:nowfixedauth:10012").metadata().get(AUTHENTICATION_APPLID)
        );
    }

    static class ServiceDefinitionProcessorMock extends ServiceDefinitionProcessor {

        private String location;

        public void setLocation(String location) {
            this.location = location;
        }

        @Override
        protected List<File> getFiles(StaticRegistrationResult context, String staticApiDefinitionsDirectories) {
            try {
                return Collections.singletonList(Paths.get(ClassLoader.getSystemResource(location).toURI()).toFile());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
