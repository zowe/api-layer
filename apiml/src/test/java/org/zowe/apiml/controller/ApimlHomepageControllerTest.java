/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.version.VersionInfo;
import org.zowe.apiml.product.version.VersionInfoDetails;
import org.zowe.apiml.product.version.VersionService;
import org.zowe.apiml.zaas.security.login.Providers;
import org.zowe.apiml.zaas.security.service.JwtSecurity;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApimlHomepageControllerTest {

    @Mock
    private DiscoveryClient discoveryClient;
    @Mock
    private VersionService versionService;
    @Mock
    private ApplicationContext applicationContext;

    private Model model;
    private ApimlHomepageController apimlHomepageController;

    @BeforeEach
    void setup() {
        Providers providers = mock(Providers.class);
        when(providers.isZosfmUsed()).thenReturn(false);
        when(applicationContext.getBean(Providers.class)).thenReturn(providers);
        when(applicationContext.getBean(JwtSecurity.class)).thenReturn(mock(JwtSecurity.class));

        ApplicationInfo applicationInfo = ApplicationInfo.builder()
            .isModulith(false)
            .authServiceId(CoreService.ZAAS.getServiceId()).build();

        apimlHomepageController = new ApimlHomepageController(discoveryClient, versionService, applicationInfo, applicationContext);
        
        model = new ConcurrentModel();
    }

    @Nested
    class WhenHomepageCalled {

        @Test
        void givenBuildVersionUnknown_thenBuildInfoShouldBeStaticText() {
            when(versionService.getVersion()).thenReturn(new VersionInfo(null, new VersionInfoDetails("unknown", "000", "abc")));

            apimlHomepageController.init();
            apimlHomepageController.home(model);

            Map<String, Object> actualModelMap = model.asMap();
            assertThat(actualModelMap, IsMapContaining.hasEntry("buildInfoText", "Build information is not available"));
        }

        @Test
        void givenSpecificBuildVersion_thenBuildInfoShouldBeGivenVersionAndNumber() {
            when(versionService.getVersion()).thenReturn(new VersionInfo(null, new VersionInfoDetails("test-version", "test-number", "abc")));

            apimlHomepageController.init();
            apimlHomepageController.home(model);

            Map<String, Object> actualModelMap = model.asMap();
            assertThat(actualModelMap, IsMapContaining.hasEntry("buildInfoText", "API ML Version test-version build # test-number"));
        }

        @Test
        void givenZoweVersion_thenZoweVersionTextShouldBeIncluded() {
            when(versionService.getVersion()).thenReturn(new VersionInfo(
                new VersionInfoDetails("2.0.0", "500", "def"),
                new VersionInfoDetails("test-version", "test-number", "abc")
            ));

            apimlHomepageController.init();
            apimlHomepageController.home(model);

            Map<String, Object> actualModelMap = model.asMap();
            assertThat(actualModelMap, IsMapContaining.hasEntry("zoweVersionText", "Zowe Version 2.0.0 build # 500"));
        }
    }
}
