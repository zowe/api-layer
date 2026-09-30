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

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.EurekaClientProvider;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.config.EurekaClientConfiguration;
import org.zowe.apiml.eurekaservice.client.config.Ssl;
import org.zowe.apiml.eurekaservice.client.util.EurekaInstanceConfigCreator;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.registry.client.HttpRegistryTransport;
import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.codec.RegistryCodec;
import org.zowe.apiml.registry.model.ServiceInstance;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsFactory;
import org.zowe.apiml.security.SecurityUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Implements {@link ApiMediationClient} interface methods for registering and unregistering REST service with
 * API Mediation Layer Discovery service. Registration method creates an instance of {@link RegistryClient}, which is
 * stored in a member variable for later use. The client instance is internally used during unregistering.
 * A getter method is provided for accessing the instance by the owning object.
 * <p>
 * The client is the API ML registry client speaking the frozen Eureka wire protocol, not the Netflix client the
 * enabler used to build here. What the enabler adds on top of it is everything a plain Java service has no
 * scheduler for: a single daemon thread that renews the lease, re-registers when the registry has forgotten the
 * instance, and keeps a cached view of the registry for services that read it through
 * {@link ApiMediationClient#getRegistryClient()}.
 */
@Slf4j
public class ApiMediationClientImpl implements ApiMediationClient {

    private final EurekaClientProvider eurekaClientProvider;
    private final EurekaClientConfigProvider eurekaClientConfigProvider;
    private final EurekaInstanceConfigCreator eurekaInstanceConfigCreator;
    private final DefaultCustomMetadataHelper defaultCustomMetadataHelper;

    private RegistryClient registryClient;
    private HttpRegistryTransport transport;
    private ScheduledExecutorService scheduler;

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
     * Registers this service with the Discovery Service using a {@link RegistryClient} initialized with the provided
     * {@link ApiMediationServiceConfig} methods parameter.
     * Successive calls to {@link #register} method without intermediate call to {@link #unregister} will be rejected with exception.
     * <p>
     * This method catches all RuntimeException, and rethrows {@link ServiceDefinitionException} checked exception.
     * <p>
     * A Discovery Service that cannot be reached is not a failed registration - the Netflix client this replaces
     * behaved the same way - it is a registration the scheduled heartbeat keeps retrying until it succeeds.
     *
     * @param config
     * @throws ServiceDefinitionException
     */
    @Override
    public synchronized void register(ApiMediationServiceConfig config) throws ServiceDefinitionException {
        if (registryClient != null) {
            throw new ServiceDefinitionException("EurekaClient was previously registered for this instance of ApiMediationClient. Call your ApiMediationClient unregister() method before attempting other registration.");
        }

        defaultCustomMetadataHelper.update(config);
        EurekaClientConfiguration clientConfiguration = eurekaClientConfigProvider.config(config);
        ServiceInstance self = eurekaInstanceConfigCreator.createServiceInstance(config);
        transport = createTransport(config, clientConfiguration);
        registryClient = eurekaClientProvider.client(transport, self);
        start(clientConfiguration);
    }

    /**
     * Unregister the service from the Discovery Service.
     */
    @Override
    public synchronized void unregister() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (registryClient != null) {
            registryClient.unregister();
            registryClient = null;
        }
        if (transport != null) {
            try {
                transport.close();
            } catch (Exception e) {
                log.debug("Closing the transport to the Discovery Service failed: {}", e.getMessage());
            }
            transport = null;
        }
    }

    private void start(EurekaClientConfiguration clientConfiguration) {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "apiml-enabler-registry-client");
            thread.setDaemon(true);
            return thread;
        });

        if (!registryClient.register()) {
            log.debug("Registration was not accepted yet; the next heartbeat will retry");
        }

        // Fetch once synchronously, so a consumer reading the cached view right after registration does not see an
        // empty registry. The Netflix client did the same in its constructor.
        registryClient.refresh();

        int renewalInterval = clientConfiguration.getLeaseRenewalIntervalInSeconds();
        int fetchInterval = clientConfiguration.getRegistryFetchIntervalSeconds();
        scheduler.scheduleWithFixedDelay(this::heartbeat, renewalInterval, renewalInterval, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::refresh, fetchInterval, fetchInterval, TimeUnit.SECONDS);
    }

    private void heartbeat() {
        try {
            registryClient.heartbeat();
        } catch (RuntimeException e) {
            // A scheduled task that throws is never run again, which would silently stop the heartbeat and let the
            // registration expire.
            log.debug("Heartbeat failed", e);
        }
    }

    private void refresh() {
        try {
            registryClient.refresh();
        } catch (RuntimeException e) {
            log.debug("Registry refresh failed", e);
        }
    }

    private HttpRegistryTransport createTransport(
        ApiMediationServiceConfig config, EurekaClientConfiguration clientConfiguration
    ) {

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
            if (Boolean.TRUE.equals(sslConfig.getVerifySslCertificatesOfServices()) &&
                Boolean.FALSE.equals(sslConfig.getNonStrictVerifySslCertificatesOfServices())) {
                builder.trustStore(sslConfig.getTrustStore())
                    .trustStoreType(sslConfig.getTrustStoreType())
                    .trustStorePassword(sslConfig.getTrustStorePassword());
            }
        }
        HttpsConfig httpsConfig = builder.build();

        HttpsFactory factory = new HttpsFactory(httpsConfig, "0.0.0.0");

        boolean strictHostnameVerification = httpsConfig.isVerifySslCertificatesOfServices()
            && !httpsConfig.isNonStrictVerifySslCertificatesOfServices();

        return new HttpRegistryTransport(
            clientConfiguration.getEurekaServerServiceUrls(),
            factory.getSslContext(),
            new RegistryCodec(),
            strictHostnameVerification,
            clientConfiguration.getEurekaServerConnectTimeoutSeconds() * 1000,
            clientConfiguration.getEurekaServerReadTimeoutSeconds() * 1000,
            config.getDiscoveryUserid(),
            config.getDiscoveryPassword() == null ? null : new String(config.getDiscoveryPassword())
        );
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

    /**
     * Can be used by the caller to work with the registry: instances, applications, etc.
     *
     * @return the inner registry client instance.
     */
    public RegistryClient getRegistryClient() {
        return registryClient;
    }
}
