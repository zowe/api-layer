/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.config.AdditionalRegistration;
import org.zowe.apiml.security.HttpsFactory;

import java.util.AbstractMap;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.eureka.EurekaClientConfigBean.DEFAULT_ZONE;

@ExtendWith(MockitoExtension.class)
public class AdditionalRegistrationTest {

    private ConnectionsConfig connectionsConfig;
    @Mock
    private CloudEurekaClient additionalClientOne;
    @Mock
    private CloudEurekaClient additionalClientTwo;
    @Mock
    private EurekaFactory eurekaFactory;
    @Mock
    ApplicationContext context;

    @BeforeEach
    void setUp() {
        connectionsConfig = new ConnectionsConfig(context);
    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    class WhenInitializingAdditionalRegistrations {
        private ConnectionsConfig configSpy;
        @Mock
        private ApplicationInfoManager manager;
        @Mock
        private EurekaClientConfig clientConfig;
        @Mock
        private HealthCheckHandler healthCheckHandler;
        @Mock
        private HttpsFactory httpsFactory;

        @Captor
        private ArgumentCaptor<EurekaClientConfigBean> clientConfigCaptor;

        private final AdditionalRegistration registration = AdditionalRegistration.builder().discoveryServiceUrls("https://another-eureka-1").build();

        @BeforeEach
        public void setUp() throws Exception {
            ReflectionTestUtils.setField(connectionsConfig,"eurekaServerUrl","https://host:2222");
            ReflectionTestUtils.setField(connectionsConfig,"httpsFactory",httpsFactory);
            configSpy = Mockito.spy(connectionsConfig);
            lenient().doReturn(httpsFactory).when(configSpy).factory();
            lenient().when(httpsFactory.getSslContext()).thenReturn(SSLContexts.custom().build());
            lenient().when(httpsFactory.getHostnameVerifier()).thenReturn(new NoopHostnameVerifier());
            lenient().when(eurekaFactory.createCloudEurekaClient(any(), any(), clientConfigCaptor.capture(), any(), any(), any())).thenReturn(additionalClientOne, additionalClientTwo);
        }

        @Test
        void shouldCreateEurekaClientForAdditionalDiscoveryUrl() {

            AdditionalEurekaClientsHolder holder = configSpy.additionalEurekaClientsHolder(manager, clientConfig, singletonList(registration), eurekaFactory, healthCheckHandler);

            assertThat(holder.getDiscoveryClients()).hasSize(1);
            EurekaClientConfigBean eurekaClientConfigBean = clientConfigCaptor.getValue();
            assertThat(eurekaClientConfigBean.getServiceUrl()).containsOnly(new AbstractMap.SimpleEntry<String, String>(DEFAULT_ZONE, "https://another-eureka-1"));
        }

        @Test
        void shouldCreateTwoAdditionalRegistrations() {
            AdditionalRegistration secondRegistration = AdditionalRegistration.builder().discoveryServiceUrls("https://another-eureka-2").build();
            AdditionalEurekaClientsHolder holder = configSpy.additionalEurekaClientsHolder(manager, clientConfig, asList(registration, secondRegistration), eurekaFactory, healthCheckHandler);

            assertThat(holder.getDiscoveryClients()).hasSize(2);
            verify(additionalClientOne).registerHealthCheck(healthCheckHandler);
            verify(additionalClientTwo).registerHealthCheck(healthCheckHandler);
        }

        @Test
        void shouldCreateInstanceInfoFromEurekaConfig() {
            EurekaInstanceConfig config = mock(EurekaInstanceConfig.class);
            when(config.getNamespace()).thenReturn("");
            when(config.getAppname()).thenReturn("gateway");

            InstanceInfo instanceInfo = new EurekaFactory().createInstanceInfo(config);

            assertThat(instanceInfo.getAppName()).isEqualTo("gateway");
        }
    }

    @Nested
    class WhenClientHolderShutDown {
        @Test
        void shouldTriggerShutdownCallToWrappedClients() {
            AdditionalEurekaClientsHolder holder = new AdditionalEurekaClientsHolder(asList(additionalClientOne, additionalClientTwo));
            holder.shutdown();

            verify(additionalClientOne).shutdown();
            verify(additionalClientTwo).shutdown();
        }

        @Test
        void shouldHandleNullsOnShutdownCall() {
            AdditionalEurekaClientsHolder holder = new AdditionalEurekaClientsHolder(null);
            holder.shutdown();

            assertThat(holder.getDiscoveryClients()).isNull();
        }
    }
}
