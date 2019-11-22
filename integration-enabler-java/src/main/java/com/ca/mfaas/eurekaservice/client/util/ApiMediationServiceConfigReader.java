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
import com.ca.mfaas.util.UrlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  This class provides facility for loading API ML service on-boarding configuration.
 *  <p/>
 *  It allows to provide one or two file names, where the first file is usually internal to the service
 *  deployment artifact or at minimum must be accessible on the service classpath. It contains basic API ML configuration
 *  based on values known at development time. Typically it is provided by the service developer and is located in the
 *  /resources folder of java project source tree. In the deployment artifact it usually can be found under /WEB-ING/classes.
 *  <p/>
 *  The second configuration file is used to externalize the configuration. It can be placed anywhere, provided that the
 *  service has access to that location. It is populated with values dependent on the deployment system environment.
 *  <p/>
 *  At service boot time, both configurations are merged, where the externalized configuration (if provided) has higher
 *  priority.
 *  <p/>
 *  The values of both configuration files can be overwritten by Java system properties.
 *
 */
@Slf4j
public class ApiMediationServiceConfigReader {

    public static final String APIML_DEFAULT_CONFIG = "apiml.config.location";
    public static final String APIML_ADDITIONAL_CONFIG = "apiml.config.additional-location";

    private static final String DEFAULT_CONFIGURATION_FILE_NAME = "/service-configuration.yml";

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]*)\\}");

    /**
     * Instance member of ThreadLocal holding a Map<String, String> of configuration properties.
     */
    private ThreadLocal<Map<String, String>> threadConfigurationContext = ThreadLocal.withInitial(() -> new HashMap<String, String>());


    private ApiMediationServiceConfig mergeConfigurations(ApiMediationServiceConfig defaultConfiguration, String additionalConfigFile/*, int mergeFlags*/)
        throws ServiceDefinitionException {

        return mergeConfigurations(defaultConfiguration, readConfigurationFile(additionalConfigFile)/*, int mergeFlags*/);
    }

    /**
     * Merges two APiMediationServiceConfig objects where the second one has higher priority, i.e replacess values into the first one.
     * @param defaultConfiguration
     * @param additionalConfiguration
     * @return
     */
    private ApiMediationServiceConfig mergeConfigurations(ApiMediationServiceConfig defaultConfiguration, ApiMediationServiceConfig  additionalConfiguration /*, int mergeFlags*/)
            throws ServiceDefinitionException {

        ApiMediationServiceConfig apimlServcieConfig = null;
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            Map<String, Object> defaultConfigPropertiesMap = mapper.convertValue(defaultConfiguration, Map.class);
            Map<String, Object> additionalConfigPropertiesMap = mapper.convertValue(additionalConfiguration, Map.class);
            if ((defaultConfigPropertiesMap != null) && (additionalConfigPropertiesMap != null)) {
                defaultConfigPropertiesMap.putAll(additionalConfigPropertiesMap);
            }

            mapper.convertValue(defaultConfigPropertiesMap, ApiMediationServiceConfig.class);
        } catch (RuntimeException rte) {
            throw new ServiceDefinitionException("Merging service basic and externalized configurations failed. See for previous exceptions: ", rte);
        }

        return apimlServcieConfig;
    }

    /**
     * Loads API ML service on-boarding configuration from single YAML file.
     * Property rewriting is applied, provided that the YAML file contains ${apiml.XXX} keys,
     * which have corresponding Java System properties
     *
     * @param internalConfigurationFileName
     * @return
     */
    public ApiMediationServiceConfig loadConfiguration(String internalConfigurationFileName)
        throws ServiceDefinitionException {

        return loadConfiguration(internalConfigurationFileName, null);
    }

    /**
     * Loads API ML on-boarding configuration build up from two YAML files. One file provides internal/basic configuration,
     * while the second one provides externalization of deployment environment dependent properties.
     * The externalized file has precedence over the basic one, i.e the properties values of the basic YAML file can be rewritten
     * by the values of the same properties defined in the externalized configuration file.
     *
     * @param internalConfigurationFileName
     * @param externalizedConfigurationFileName
     * @return
     */
    public ApiMediationServiceConfig loadConfiguration(String internalConfigurationFileName, String externalizedConfigurationFileName)
            throws ServiceDefinitionException {

        if (internalConfigurationFileName == null) {
            internalConfigurationFileName = DEFAULT_CONFIGURATION_FILE_NAME;
        }

        ApiMediationServiceConfig serviceConfig = readConfigurationFile(internalConfigurationFileName);

        if (externalizedConfigurationFileName != null) {
            serviceConfig = mergeConfigurations(serviceConfig, externalizedConfigurationFileName);
        }

        // Set instance ipAddress if required by Eureka and not set in the configuration files
        serviceConfig.setServiceIpAddress(UrlUtils.getIpAddressFromUrl(serviceConfig.getBaseUrl()));

        return serviceConfig;
    }

    /**
     * First locates the file and if successfull, reads its contents into a {@link ApiMediationServiceConfig}
     * @param fileName
     * @return
     */
    public ApiMediationServiceConfig readConfigurationFile(String fileName)
            throws ServiceDefinitionException {

        File file = locateConfigFile(fileName);

        return (file != null) ? readConfigurationFile(file) : null;
    }

    /**
     * Tries to locate or in other words to verify that a file with name 'fileName' exists.
     * First tries to find the file as a resource somewhere on the application or System classpath.
     * Then tries to locate it using 'fileName' as as relative path
     * The final attempt is to locate the file using the 'fileName' an absolute path.  resolved from .
     *
     * This method never throw exceptions.
     * Returns null If fileName is null or file is not found neither as Java (system) resource, nor as file on the file system.
     *
     * @param fileName
     * @return
     */
    private File locateConfigFile(String fileName) {
        if (fileName == null) {
            return null;
        }
        // Try to find the file as a resource - application local or System resource
        URL fileUrl = null;
        try {
            fileUrl = getClass().getResource(fileName);
            if (fileUrl == null) {
                log.debug(String.format("File resource [%s] can't be found by this class classloader. We'll try with SystemClassLoader...", fileName));

                fileUrl = ClassLoader.getSystemResource(fileName);
                if (fileUrl == null) {
                    log.debug(String.format("File resource [%s] can't be found by SystemClassLoader.", fileName));
                }
            }
        } catch (Throwable t) {
            // Silently swallow the exceptions and try to find the file on the File Sytem
            log.debug(String.format("File [%s] can't be found as Java resource. Exception was caught with the following message: [%s]", fileName) + t.getMessage());
        }

        File file = null;
        try {
            if (fileUrl != null) {
                file = new File(fileUrl.getFile());
            } else {
                Path path = Paths.get(fileName);
                File aFile = path.toFile();
                if (aFile.canRead()) {
                    file = aFile;
                } else {
                    if (!path.isAbsolute()) {
                        // Relative path can exist on multiple root file systems. Try all of them.
                        for (File root : File.listRoots()) {
                            Path resolvedPath = root.toPath().resolve(path);
                            aFile = resolvedPath.toFile();
                            if (aFile.canRead()) {
                                file = aFile;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // Silently swallow the exceptions and try to find the file on the File Sytem
            log.debug(String.format("File [%s] can't be found as file system resource. Exception was caught with the following message: [%s]", fileName) + t.getMessage());
        }

        return file;
    }

    /**
     * Reads configuration form a single YAML file.
     * Properties values rewriting takes place using Java System properties with keys prefixed with "apiml."
     *
     * @param file
     * @return
     */
    public ApiMediationServiceConfig readConfigurationFile(File file) throws ServiceDefinitionException {
        ApiMediationServiceConfig configuration = null;

        if (file != null) {
            try {
                String data = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                StringBuilder configFileDataBuilder = resolveExpressions(data, threadConfigurationContext.get());
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                configuration = mapper.readValue(configFileDataBuilder.toString(), ApiMediationServiceConfig.class);
            } catch (FileNotFoundException | NoSuchFileException e) {
                throw new ServiceDefinitionException(String.format("File [%s] doesn't exist", file.getName()), e);
            } catch (IOException e) {
                throw new ServiceDefinitionException(String.format("File [%s] can't be parsed as ApiMediationServiceConfig", file.getName()));
            }
        }

        return configuration;
    }

    /**
     * Utility method for setting this thread configuration context with ServletContext parameters who's keys are prefixed with "apiml."
     *
     * @param servletContext
     */
    public Map<String, String> setApiMlServiceContext(ServletContext servletContext) {
        Map<String, String> threadContextMap = threadConfigurationContext.get();

        /*
           Because this class is intended to be used mainly in web containers it is expected that
           the thread instances belong to a thread pool.
           We need then to clean the threadConfigurationContext before loading new configuration parameters from servlet context.
        */
        if (!threadContextMap.isEmpty()) {
            threadContextMap.clear();
        }

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
     * Substitutes properties values if corresponding properties keys are found in the expression.
     *
     * @param expression
     * @param properties
     * @return
     */
    private static StringBuilder resolveExpressions(String expression, Map<String, String> properties) {
        StringBuilder result = new StringBuilder(expression.length());
        int i = 0;
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        while (matcher.find()) {
            // Strip leading "${" and trailing "}" off.
            result.append(expression.substring(i, matcher.start()));
            String property = matcher.group();
            property = property.substring(2, property.length() - 1);
            if (properties.containsKey(property)) {
                //look up property and replace
                property = properties.get(property);
            } else {
                //property not found, don't replace
                property = matcher.group();
            }
            result.append(property);
            i = matcher.end();
        }
        result.append(expression.substring(i));
        return result;
    }

    /**
     * The initialize method, uses servlet context parameters to build service configuration for on-boarding with API ML discovery service.
     *
     * It uses {@link ApiMediationServiceConfigReader} to build {@link ApiMediationServiceConfig} configuration.
     *
     * The service configuration is provided by YAML files. The names of of the configuration files are passed using
     * servlet context parameters with following parameter names:
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
         * Set Java system properties from ServletContext parameters starting with "apiml." prefix.
         * If rewriting is not used, it is not necessary to call {@link ApiMediationServiceConfigReader#setSystemProperties}
         */
        setApiMlServiceContext(context);

        /*
         *  Get default configuration file name from ServletContext init parameter.
         *  If null, ApiMediationServiceConfigReader will use "/service-configuration.yml" as default.
         */
        String basicConfigurationFileName = context.getInitParameter(APIML_DEFAULT_CONFIG);
        if (basicConfigurationFileName == null) {
            basicConfigurationFileName = System.getProperty(APIML_DEFAULT_CONFIG);
        }

        /*
         * (Optional) Get externalized configuration file name from ServletContext init parameter.
         */
        String externalConfigurationFileName = context.getInitParameter(APIML_ADDITIONAL_CONFIG);
        if (externalConfigurationFileName == null) {
            externalConfigurationFileName = System.getProperty(APIML_ADDITIONAL_CONFIG);
        }

        /*
         * Instantiate configuration reader and call loadConfiguration method with both config file names initialized above.
         */
        ApiMediationServiceConfig apimlConfig = loadConfiguration(basicConfigurationFileName, externalConfigurationFileName);

        return apimlConfig;
    }
}
