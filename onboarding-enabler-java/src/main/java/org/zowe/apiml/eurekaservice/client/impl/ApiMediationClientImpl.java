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
import com.netflix.appinfo.HealthCheckCallbackToHandlerBridge;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.*;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import com.netflix.discovery.shared.transport.jersey3.Jersey3TransportClientFactories;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.EurekaClientProvider;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.Ssl;
import org.zowe.apiml.eurekaservice.client.util.EurekaInstanceConfigCreator;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsFactory;
import org.zowe.apiml.security.SecurityUtils;


/**
 * Implements {@link ApiMediationClient} interface methods for registering and unregistering REST service with
 * API Mediation Layer Discovery service. Registration method creates an instance of {@link com.netflix.discovery.EurekaClient}, which is
 * stored in a member variable for later use. The client instance is internally used during unregistering.
 * A getter method is provided for accessing the instance by the owning object.
 * Basically it gathers the configuration and provides it to the DiscoveryClient.
 * So here we may want to test whether we properly gather the configuration.
 * And verify internal issues if something is wrong.
 * <p>
 * The issue with DiscoveryService and its behavior is tested in the Eureka.
 */
public class ApiMediationClientImpl implements ApiMediationClient {

    private final EurekaClientProvider eurekaClientProvider;
    private final EurekaClientConfigProvider eurekaClientConfigProvider;
    private final EurekaInstanceConfigCreator eurekaInstanceConfigCreator;
    private final DefaultCustomMetadataHelper defaultCustomMetadataHelper;

    private EurekaClient eurekaClient;

    public ApiMediationClientImpl() {
        this(new DiscoveryClientProvider());
    }

    public ApiMediationClientImpl(EurekaClientProvider eurekaClientProvider) {
        this(eurekaClientProvider, new ApiMlEurekaClientConfigProvider());
    }

    public ApiMediationClientImpl(
        EurekaClientProvider eurekaClientProvider, EurekaClientConfigProvider eurekaClientConfigProvider
    ) {
        this(eurekaClientProvider, eurekaClientConfigProvider, new EurekaInstanceConfigCreator());
    }

    public ApiMediationClientImpl(
        EurekaClientProvider eurekaClientProvider,
        EurekaClientConfigProvider eurekaClientConfigProvider,
        EurekaInstanceConfigCreator instanceConfigCreator
    ) {
        this(eurekaClientProvider, eurekaClientConfigProvider, instanceConfigCreator, new DefaultCustomMetadataHelper());
    }

    public ApiMediationClientImpl(
        EurekaClientProvider eurekaClientProvider,
        EurekaClientConfigProvider eurekaClientConfigProvider,
        EurekaInstanceConfigCreator instanceConfigCreator,
        DefaultCustomMetadataHelper defaultCustomMetadataHelper
    ) {
        this.eurekaClientProvider = eurekaClientProvider;
        this.eurekaClientConfigProvider = eurekaClientConfigProvider;
        this.eurekaInstanceConfigCreator = instanceConfigCreator;
        this.defaultCustomMetadataHelper = defaultCustomMetadataHelper;
    }

    /**
     * Registers this service with Eureka server using EurekaClient which is initialized with the provided {@link ApiMediationServiceConfig} methods parameter.
     * Successive calls to {@link #register} method without intermediate call to {@linl #unregister} will be rejected with exception.
     * <p>
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

        defaultCustomMetadataHelper.update(config);
        EurekaClientConfig clientConfiguration = eurekaClientConfigProvider.config(config);
        ApplicationInfoManager infoManager = initializeApplicationInfoManager(config);
        eurekaClient = initializeEurekaClient(infoManager, clientConfiguration, config);
        if (eurekaClient != null) {
            eurekaClient.registerHealthCheck(new HealthCheckCallbackToHandlerBridge());
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
        if (sslConfig != null) {
            updateStorePaths(sslConfig);
            builder.protocol(sslConfig.getProtocol());
            if (Boolean.TRUE.equals(sslConfig.getEnabled())) {
                builder.keyAlias(sslConfig.getKeyAlias())
                    .keyStore(sslConfig.getKeyStore())
                    .keyPassword(sslConfig.getKeyPassword())
                    .keyStorePassword(sslConfig.getKeyStorePassword())
                    .keyStoreType(sslConfig.getKeyStoreType());
            }

            builder.verifySslCertificatesOfServices(Boolean.TRUE.equals(sslConfig.getVerifySslCertificatesOfServices()));
            builder.nonStrictVerifySslCertificatesOfServices(Boolean.TRUE.equals(sslConfig.getNonStrictVerifySslCertificatesOfServices()));
            if (Boolean.TRUE.equals(sslConfig.getVerifySslCertificatesOfServices()) ||
                Boolean.FALSE.equals(sslConfig.getNonStrictVerifySslCertificatesOfServices())) {
                builder.trustStore(sslConfig.getTrustStore())
                    .trustStoreType(sslConfig.getTrustStoreType())
                    .trustStorePassword(sslConfig.getTrustStorePassword());
            }
        }
        HttpsConfig httpsConfig = builder.build();

        HttpsFactory factory = new HttpsFactory(httpsConfig);

        AbstractDiscoveryClientOptionalArgs<?> args = new Jersey3DiscoveryClientOptionalArgs();
        args.setSSLContext(factory.getSslContext());
        args.setHostnameVerifier(factory.getHostnameVerifier());
        TransportClientFactories<?> transportClientFactories = Jersey3TransportClientFactories.getInstance();
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        return this.eurekaClientProvider.client(applicationInfoManager, clientConfig, transportClientFactories, args);
    }

    void updateStorePaths(Ssl config) {
        if (SecurityUtils.isKeyring(config.getKeyStore())) {
            config.setKeyStore(SecurityUtils.formatKeyringUrl(config.getKeyStore()));
            if (config.getKeyStorePassword() == null) config.setKeyStorePassword("password".toCharArray());
        }
        if (SecurityUtils.isKeyring(config.getTrustStore())) {
            config.setTrustStore(SecurityUtils.formatKeyringUrl(config.getTrustStore()));
            if (config.getTrustStorePassword() == null) config.setTrustStorePassword("password".toCharArray());
        }
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
