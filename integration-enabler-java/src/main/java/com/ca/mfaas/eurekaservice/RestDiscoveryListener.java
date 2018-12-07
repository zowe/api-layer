/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.util.ResourceBundle;

public class RestDiscoveryListener implements ServletContextListener {
    private static ResourceBundle eurekaProperties = ResourceBundle.getBundle("eureka-client");
    private static ApplicationInfoManager applicationInfoManager;
    private static EurekaClient eurekaClient;

    private static synchronized ApplicationInfoManager initializeApplicationInfoManager(EurekaInstanceConfig instanceConfig) {
        if (applicationInfoManager == null) {
            InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
            InstanceInfo.Builder builder = new InstanceInfo.Builder(instanceInfo);
            String serviceId = eurekaProperties.getString("eureka.name");
            String hostname = eurekaProperties.getString("eureka.service.hostname");
            String port = eurekaProperties.getString("eureka.port");
            String sslEnabled = eurekaProperties.getString("ssl.enabled");
            switch (sslEnabled) {
                case "false":
                    builder.enablePort(InstanceInfo.PortType.SECURE, false).enablePort(InstanceInfo.PortType.UNSECURE, true)
                        .setPort(Integer.parseInt(eurekaProperties.getString("eureka.port")));
                    break;
                case "true":
                    builder.enablePort(InstanceInfo.PortType.SECURE, true).enablePort(InstanceInfo.PortType.UNSECURE, false)
                        .setSecurePort(Integer.parseInt(eurekaProperties.getString("eureka.securePort")));
                    break;
            }
            String instanceId = String.format("%s:%s:%s", hostname, serviceId, port);
            InstanceInfo updatedInstanceInfo = builder.setInstanceId(instanceId).setHostName(hostname).build();
            applicationInfoManager = new ApplicationInfoManager(instanceConfig, updatedInstanceInfo);
        }
        return applicationInfoManager;
    }

    private static synchronized EurekaClient initializeEurekaClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig) {
        if (eurekaClient == null) {
            eurekaClient = new DiscoveryClient(applicationInfoManager, clientConfig);
        }
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        return eurekaClient;
    }

    public void contextInitialized(ServletContextEvent sce) {
        // create the client
        ApplicationInfoManager infoManager = initializeApplicationInfoManager(new MyDataCenterInstanceConfig());
        initializeEurekaClient(infoManager, new DefaultEurekaClientConfig());
    }

    public void contextDestroyed(ServletContextEvent sce) {
        // shutdown the client
        eurekaClient.shutdown();
    }
}
