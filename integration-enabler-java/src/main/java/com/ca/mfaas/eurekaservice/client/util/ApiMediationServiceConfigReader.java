/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.exception.ServiceDefinitionException;
import com.ca.mfaas.util.FileUtils;
import com.ca.mfaas.util.ObjectUtil;
import com.ca.mfaas.util.StringUtils;
import com.ca.mfaas.util.UrlUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 *  This class implements a facility for loading API ML service on-boarding configuration.
 *  <p>
 *  The implementation provides several methods for building the service configuration:
 *      <ul>
 *          <li>single filename in YAML format</li>
 *          <li>two file names in YAML format</li>
 *          <li>ServletContext initialized with servlet init parameters containing one or two file names under predefined
 *          keys and/or additional parameters to overwrite some of the properties contained in the configuration files
 *          </li>
 *          <li>String containing the YAML as text</li>
 *          <li>Java System properties which can be provided on the command line at service boot time</li>
 *      </ul>
 *  The file names can point to files placed anywhere in the file system, provided that the service has access to them.
 *  </p>
 *
 *  <p>
 *  The first file is usually internal to the service deployment artifact and should be located on
 *  the service classpath. It contains basic API ML configuration containing values known at development time.
 *  Typically it is provided by the service developer and is located in the /resources folder of java project source
 *  tree. In the deployment artifact it usually can be found under /WEB-INF/classes.
 *  </p>
 *
 *  <p>
 *  The second configuration file is used to externalize the configuration. It is populated with values dependent on
 *  the target deployment environment. The file can be placed in arbitrary location, where the order of searching it is as follows:
 *      <ul>
 *          <li>Service classpath</li>
 *          <li>System classpath classpath</li>
 *          <li>Absolute path</li>
 *          <li>Path relative to the Web server home directory</li>
 *          <li>Path relative to the User home directory</li>
 *          <li>Path relative to any of the file system roots</li>
 *      </ul>
 *
 *  When the service web context is initialized, the on-boarding configuration is build from the one or two configurations,
 *  where the externalized configuration (if provided) has higher priority.
 *  </p>
 *
 *  <p>
 *  The values of the properties configured in both files can be overwritten by Java System properties and/or
 *  ServletContext parameters. Set a parameter value in the YAML file to ${apiml.your.config.key} and provide the actual
 *  value  under the same key in a servlet context init parameter:
 *      <Parameter name="apiml.your.very.own.key" value="the-actual-property-value" />
 *
 *  , or in the corresponding Java System property.
 *  </p>
 *
 */
public class ApiMediationServiceConfigReader {

    public static final String APIML_DEFAULT_CONFIG = "apiml.config.location";

    public static final String APIML_ADDITIONAL_CONFIG = "apiml.config.additional-location";

    private static final String DEFAULT_CONFIGURATION_FILE_NAME = "/service-configuration.yml";

    /**
     * Instance object mapper. Initialized in constructor.
     */
    private ObjectMapper objectMapper;

    /**
     * Instance member of ThreadLocal holding a Map<String, String> of configuration properties.
     */
    private ThreadLocal<Map<String, String>> threadConfigurationContext = ThreadLocal.withInitial(HashMap::new);


    /**
     * Default constructor.
     */
    public ApiMediationServiceConfigReader() {
        objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.setDefaultMergeable(true);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(NON_NULL, NON_ABSENT));
    }


    /**
     * Merges two APiMediationServiceConfig objects, where the second one has higher priority, i.e replaces values into the first one.
     *
     * @param defaultConfiguration
     * @param additionalConfiguration
     * @return
     */
    public ApiMediationServiceConfig mergeConfigurations(ApiMediationServiceConfig defaultConfiguration, ApiMediationServiceConfig additionalConfiguration) {

        Map<String, Object> defaultConfigPropertiesMap = objectMapper.convertValue(defaultConfiguration, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = objectMapper.convertValue(additionalConfiguration, Map.class);
        Map<String, Object> config = ObjectUtil.mergeConfigurations(defaultConfigPropertiesMap, additionalConfigPropertiesMap);

        return objectMapper.convertValue(config, ApiMediationServiceConfig.class);
    }

    /**
     * Loads API ML service on-boarding configuration from single YAML file.
     * Property rewriting is applied, provided that the YAML file contains ${apiml.XXX} keys,
     * which have corresponding Java System property or ServletContext ini parameter key.
     *
     * @param internalConfigurationFileName
     * @return
     */
    public ApiMediationServiceConfig loadConfiguration(String internalConfigurationFileName)
        throws ServiceDefinitionException {

        return loadConfiguration(internalConfigurationFileName, null, false);
    }

    /**
     * Loads API ML on-boarding configuration build up from two YAML files. One file provides internal/basic configuration,
     * while the second one provides externalization of deployment environment dependent properties.
     * The externalized file has precedence over the basic one, i.e the properties values of the basic YAML file can be rewritten
     * by the values of the same properties defined in the externalized configuration file.
     *
     * Note: This method implementation relies on caller to set a map with API ML related System properties and
     * context parameters provided by system admin. Service context parameters take precedence over system properties.
     *
     * @param internalConfigFileName
     * @param externalizedConfigFileName
     * @return
     */
    public ApiMediationServiceConfig loadConfiguration(String internalConfigFileName, String externalizedConfigFileName)
        throws ServiceDefinitionException {

        return loadConfiguration(internalConfigFileName, externalizedConfigFileName, false);
    }

    private ApiMediationServiceConfig loadConfiguration(String internalConfigFileName, String externalizedConfigFileName, boolean isInitialized)
            throws ServiceDefinitionException {

        /*
         * Loading new configuration. Clean context map which might be kept in ThreadLocal object from previous configs.
         */
        if (!isInitialized) {
            initializeContextMap();

            /*
             * Store API ML relevant ("apiml." prefixed) Java system properties to a Map in ThreadLocal
             */
            setApiMlSystemProperties();
        }

        if (internalConfigFileName == null) {
            internalConfigFileName = DEFAULT_CONFIGURATION_FILE_NAME;
        }

        ApiMediationServiceConfig serviceConfig = buildConfiguration(internalConfigFileName);

        if (externalizedConfigFileName != null) {
            ApiMediationServiceConfig externalizedConfig = buildConfiguration(externalizedConfigFileName);

            ApiMediationServiceConfig mergedConfig = null;
            if (externalizedConfig != null) {
                mergedConfig = mergeConfigurations(serviceConfig, externalizedConfig);
            }

            if (mergedConfig != null) {
                serviceConfig = mergedConfig;
            }
        }

        setServiceIpAddress(serviceConfig);

        return serviceConfig;
    }

    private void setServiceIpAddress(ApiMediationServiceConfig serviceConfig) throws ServiceDefinitionException {
        // Set instance ipAddress if required by Eureka and not set in the configuration files
        if ((serviceConfig != null) && (serviceConfig.getServiceIpAddress() == null)) {
            String urlString = serviceConfig.getBaseUrl();
            try {
                serviceConfig.setServiceIpAddress(UrlUtils.getIpAddressFromUrl(urlString));
            } catch (MalformedURLException e) {
                throw new ServiceDefinitionException(String.format("%s is not a valid URL.", urlString), e);
            } catch (UnknownHostException e) {
                throw new ServiceDefinitionException(String.format("URL %s contains unknown hostname.", urlString), e);
            }
        }
    }

    /**
     * Creates a {@link ApiMediationServiceConfig} from provided config data string.
     * By default a YAML format string is expected.
     *
     * @param fileName
     * @return
     * @throws ServiceDefinitionException
     */
    public ApiMediationServiceConfig buildConfiguration(String fileName) throws ServiceDefinitionException {
        return buildConfiguration(fileName, threadConfigurationContext.get());
    }

    /**
     * Creates a {@link ApiMediationServiceConfig} from provided config data string.
     * By default a YAML format string is expected.
     *
     * @param fileName
     * @return
     * @throws ServiceDefinitionException
    */
    public ApiMediationServiceConfig buildConfiguration(String fileName, Map<String, String> properties) throws ServiceDefinitionException {
        if (fileName == null) {
            return null;
        }

        String configData = null;
        try {
            configData = FileUtils.readConfigurationFile(fileName);
        } catch (IOException e) {
            throw new ServiceDefinitionException(String.format("Configuration data can't be read from file %s.", fileName), e);
        }
        configData = StringUtils.resolveExpressions(configData, properties);

        try {
            return objectMapper.readValue(configData, ApiMediationServiceConfig.class);
        } catch (IOException e) {
            throw new ServiceDefinitionException("Configuration data can't be parsed as ApiMediationServiceConfig.", e);
        }
    }

    /**
     * Utility method for setting this thread configuration context with ServletContext parameters who's keys are prefixed with "apiml."
     *
     * @param servletContext
     */
    public Map<String, String> setApiMlServiceContext(ServletContext servletContext) {
        Map<String, String> threadContextMap = ObjectUtil.getThreadContextMap(threadConfigurationContext);

        Enumeration<String> paramNames = servletContext.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            String value = servletContext.getInitParameter(param);
            if (param.startsWith("apiml.")) {
                threadContextMap.put(param, value);
            }
        }
        return threadContextMap;
    }

    /**
     * Uses {@link ApiMediationServiceConfigReader} to build {@link ApiMediationServiceConfig} configuration.
     *
     * This is the most flexible API ML service configuration method using several configuration sources in the following order:
     *     - internal configuration file with default name service-config.yml
     *     - external configuration file if provided as Java system property or servlet context parameter
     *     - Java system properties prefixed with "apiml."
     *     - Servlet context parameters prefixed with "apiml."
     *
     * The internal and external service configuration is provided by YAML files. The names of of the configuration files can be set using
     * Java system properties or servlet context parameters with following parameter names:
     *
     *  <ul>
     *      <li>
     *          apiml.config.location - basic (or internal) configuration file provided by service developer.
     *          By default is part of the service war or jar file but can be placed anywhere on the classpath to be accessed as java application resource.
     *          </li>
     *      <li>
     *          apiml.config.additional-location - externalized configuration file provided by service deployer.
     *          Can be placed anywhere on a file system accessible from the service application.
     *      </li>
     *  </ul>
     *
     * If the basic configuration file name is not provided, the called API ML enabler configuration loader
     * will use "/service-configuration.yml" as default name.
     *
     * The externalized configuration file is optional. The deployer of the service may decide not to use it. Consequently
     * the deployment environment dependent configuration values can be provided as additional servlet context
     * parameters with key names prefixed by "apiml.". They will be transferred to Java system properties
     * and can be used to substitute corresponding service configuration properties. This approach can be used only
     * for configuration properties which are tokenized in the YAML configuration file.
     *
     * For example if the service developer would define/tokenize the serviceId parameter as:
     *      serviceId: ${apiml.serviceId}
     *
     *  The service deployer must define following context parameter:
     *      <Parameter name="apiml.serviceId" value="helloapiml" />
     *
     * The value "helloapiml" will then be used as "serviceId".
     *
     * This properties overwriting mechanism can be also used for the externalized configuration file.
     *
     * @param context
     */
    public ApiMediationServiceConfig loadConfiguration(ServletContext context) throws ServiceDefinitionException {

        /*
         * Loading new configuration. Clean context map which might be kept in ThreadLocal object from previous configs.
         */
        initializeContextMap();

        /*
         * Store API ML relevant ("apiml." prefixed) Java system properties to a Map in ThreadLocal
         */
        setApiMlSystemProperties();

        /*
         * Set Java system properties from ServletContext parameters starting with "apiml." prefix.
         * If rewriting is not used, it is not necessary to call {@link ApiMediationServiceConfigReader#setSystemProperties}
         */
        setApiMlServiceContext(context);

        Map<String, String> threadContextMap = ObjectUtil.getThreadContextMap(threadConfigurationContext);

        /*
         *  Get default configuration file name from ServletContext init parameter.
         *  If null, ApiMediationServiceConfigReader will use "/service-configuration.yml" as default.
         */
        String basicConfigurationFileName = threadContextMap.get(APIML_DEFAULT_CONFIG);
        if (basicConfigurationFileName == null) {
            basicConfigurationFileName = threadContextMap.get(APIML_DEFAULT_CONFIG);
        }

        /*
         * (Optional) Get externalized configuration file name from ServletContext init parameter.
         */
        String externalConfigurationFileName = threadContextMap.get(APIML_ADDITIONAL_CONFIG);
        if (externalConfigurationFileName == null) {
            externalConfigurationFileName = threadContextMap.get(APIML_ADDITIONAL_CONFIG);
        }

        /*
         * return result of loadConfiguration method with both config file names initialized above.
         */
        return loadConfiguration(basicConfigurationFileName, externalConfigurationFileName, true);
    }

    /**
     * Add/Replace all system properties prefixed with "apiml." to the apiml context map stored in ThreadLocal.
     *
     * WARNING: This method could rewrite previously set Servlet context parameters
     *
     */
    private Map<String, String> setApiMlSystemProperties() {

        Map<String, String> threadContextMap = ObjectUtil.getThreadContextMap(threadConfigurationContext);

        Enumeration<?> propertyNames = System.getProperties().propertyNames();
        while (propertyNames.hasMoreElements()) {
            String param = (String)propertyNames.nextElement();
            String value = System.getProperties().getProperty(param);
            if (param.startsWith("apiml.")) {
                threadContextMap.put(param, value);
            }
        }

        return threadContextMap;
    }

    /**
     *  Because this class is intended to be used mainly in web containers it is expected that
     *  the thread instances belong to a thread pool.
     *  We need then to clean the threadConfigurationContext before loading new configuration parameters from servlet context.
     */
    private void initializeContextMap() {
        threadConfigurationContext.remove();
    }
}
