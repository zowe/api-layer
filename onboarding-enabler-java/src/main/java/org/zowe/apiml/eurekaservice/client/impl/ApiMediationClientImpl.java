/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.eurekaservice.client.impl;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.EurekaClientProvider;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.EurekaClientConfiguration;
import org.zowe.apiml.eurekaservice.client.config.Ssl;
import org.zowe.apiml.eurekaservice.client.util.EurekaInstanceConfigCreator;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsFactory;
import org.zowe.apiml.util.MapUtils;

/**
 *  Implements {@link ApiMediationClient} interface methods for registering and unregistering REST service with
 *  API Mediation Layer Discovery service. Registration method creates an instance of {@link com.netflix.discovery.EurekaClient}, which is
 *  stored in a member variable for later use. The client instance is internally used during unregistering.
 *  A getter method is provided for accessing the instance by the owning object.
 *  Basically it gathers the configuration and provides it to the DiscoveryClient.
 *  So here we may want to test whether we properly gather the configuration.
 *  And verify internal issues if something is wrong.
 *
 *  The issue with DiscoveryService and its behavior is tested in the Eureka.
 */
public class ApiMediationClientImpl implements ApiMediationClient {
    private EurekaClientProvider eurekaClientProvider;
    private EurekaClient eurekaClient;
    private final EurekaInstanceConfigCreator eurekaInstanceConfigCreator = new EurekaInstanceConfigCreator(new MapUtils());

    public ApiMediationClientImpl() {
        eurekaClientProvider = new DiscoveryClientProvider();
    }

    public ApiMediationClientImpl(EurekaClientProvider eurekaClientProvider) {
        this.eurekaClientProvider = eurekaClientProvider;
    }

    /**
     * Registers this service with Eureka server using EurekaClient which is initialized with the provided {@link ApiMediationServiceConfig} methods parameter.
     * Successive calls to {@link #register} method without intermediate call to {@linl #unregister} will be rejected with exception.
     *
     * This method catches all RuntimeException, and rethrows {@link ServiceDefinitionException} checked exception.
     *
     * @param config
     * @throws ServiceDefinitionException
     */
    @Override
    public synchronized void register(ApiMediationServiceConfig config) throws ServiceDefinitionException {
        if (eurekaClient != null) {
            throw new ServiceDefinitionException("EurekaClient was previously registered for this instance of ApiMediationClient. Call your ApiMediationClient unregister() method before attempting other registration.");
        }

        EurekaClientConfiguration clientConfiguration = new EurekaClientConfiguration(config);
        try {
            ApplicationInfoManager infoManager = initializeApplicationInfoManager(config);
            eurekaClient = initializeEurekaClient(infoManager, clientConfiguration, config);
        } catch (RuntimeException rte) {
            throw new ServiceDefinitionException("Registration was not successful due to unexpected RuntimeException: ", rte);
        }
    }

    /**
     * Unregister the service from Eureka server.
     */
    @Override
    public synchronized void unregister() {
        if (eurekaClient != null) {
            eurekaClient.shutdown();
        }
        eurekaClient = null;
    }

    /**
     * Create and initialize EurekaClient instance.
     *
     * @param applicationInfoManager
     * @param clientConfig
     * @param config
     * @return Initialized {@link DiscoveryClient} instance - an implementation of {@link EurekaClient}
     */
    private EurekaClient initializeEurekaClient(
        ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig, ApiMediationServiceConfig config) {

        Ssl sslConfig = config.getSsl();

        HttpsConfig.HttpsConfigBuilder builder = HttpsConfig.builder();
        builder.protocol(sslConfig.getProtocol());

        if (Boolean.TRUE.equals(sslConfig.getEnabled())) {
            builder.keyAlias(sslConfig.getKeyAlias())
                   .keyStore(sslConfig.getKeyStore())
                   .keyPassword(sslConfig.getKeyPassword())
                   .keyStorePassword(sslConfig.getKeyStorePassword())
                   .keyStoreType(sslConfig.getKeyStoreType());
        }

        builder.verifySslCertificatesOfServices(Boolean.TRUE.equals(sslConfig.getVerifySslCertificatesOfServices()));
        if (Boolean.TRUE.equals(sslConfig.getVerifySslCertificatesOfServices())) {
            builder.trustStore(sslConfig.getTrustStore())
                   .trustStoreType(sslConfig.getTrustStoreType())
                   .trustStorePassword(sslConfig.getTrustStorePassword());
        }

        HttpsConfig httpsConfig = builder.build();

        HttpsFactory factory = new HttpsFactory(httpsConfig);
        EurekaJerseyClient eurekaJerseyClient = factory.createEurekaJerseyClientBuilder(
            config.getDiscoveryServiceUrls().get(0), config.getServiceId()).build();

        AbstractDiscoveryClientOptionalArgs args = new DiscoveryClient.DiscoveryClientOptionalArgs();
        args.setEurekaJerseyClient(eurekaJerseyClient);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        return this.eurekaClientProvider.client(applicationInfoManager, clientConfig, args);
    }

    private ApplicationInfoManager initializeApplicationInfoManager(ApiMediationServiceConfig config) throws ServiceDefinitionException {
        EurekaInstanceConfig eurekaInstanceConfig = eurekaInstanceConfigCreator.createEurekaInstanceConfig(config);
        InstanceInfo instanceInformation = new EurekaConfigBasedInstanceInfoProvider(eurekaInstanceConfig).get();
        return new ApplicationInfoManager(eurekaInstanceConfig, instanceInformation);
    }

    /**
     * Can be used by the caller to work with Eureka registry instances, regions, applications ,etc.
     *
     * @return the inner EurekaClient instance.
     */
    public EurekaClient getEurekaClient() {
        return eurekaClient;
    }
}
