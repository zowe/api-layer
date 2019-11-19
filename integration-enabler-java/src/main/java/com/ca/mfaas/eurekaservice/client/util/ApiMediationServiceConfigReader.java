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
import com.ca.mfaas.utils.UrlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import javax.naming.*;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
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


    private ApiMediationServiceConfig mergeConfigurations(ApiMediationServiceConfig defaultConfiguration, String additionalConfigFile/*, int mergeFlags*/) {
        return mergeConfigurations(defaultConfiguration, readConfigurationFile(additionalConfigFile)/*, int mergeFlags*/);
    }

    /**
     * Merges two APiMediationServiceConfig objects where the second one has higher priority, i.e replacess values into the first one.
     * @param defaultConfiguration
     * @param additionalConfiguration
     * @return
     */
    private ApiMediationServiceConfig mergeConfigurations(ApiMediationServiceConfig defaultConfiguration, ApiMediationServiceConfig  additionalConfiguration /*, int mergeFlags*/) {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> defaultConfigPropertiesMap = mapper.convertValue(defaultConfiguration, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = mapper.convertValue(additionalConfiguration, Map.class);

        if (additionalConfigPropertiesMap != null) {
            defaultConfigPropertiesMap.putAll(additionalConfigPropertiesMap);
        }

        return mapper.convertValue(defaultConfigPropertiesMap, ApiMediationServiceConfig.class);
    }

    /**
     * Utility static method for setting Java System properties form ServletContext parameters who's keys are prefixed with "apiml."
     *
     * @param context
     */
    public static void setAPIMLSystemProperties(ServletContext context) {
        Enumeration<String> paramNames = context.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            String value = context.getInitParameter(param);
            if (param.startsWith("apiml.")) {
                System.setProperty(param, value);
            }
        }
    }


    /**
     * Loads API ML service on-boarding configuration from single YAML file.
     * Property rewriting is applied, provided that the YAML file contains ${apiml.XXX} keys,
     * which have corresponding Java System properties
     *
     * @param internalConfigurationFileName
     * @return
     */
    public ApiMediationServiceConfig loadConfiguration(String internalConfigurationFileName) {
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
    public ApiMediationServiceConfig loadConfiguration(String internalConfigurationFileName, String externalizedConfigurationFileName) {

        if (internalConfigurationFileName == null) {
            internalConfigurationFileName = DEFAULT_CONFIGURATION_FILE_NAME;
        }

        ApiMediationServiceConfig serviceConfig = readConfigurationFile(internalConfigurationFileName);

        if (externalizedConfigurationFileName != null) {
            serviceConfig = mergeConfigurations(serviceConfig, externalizedConfigurationFileName);
        }

        // Set instance ipAddress if required by Eureka and not set in the configuration files
        setInstanceIpAddress(serviceConfig);

        return serviceConfig;
    }

    /**
     * First locates the file and if successfull, reads its contents into a {@link ApiMediationServiceConfig}
     * @param fileName
     * @return
     */
    public ApiMediationServiceConfig readConfigurationFile(String fileName) {

        File file = locateConfigFile(fileName);

        return (file != null) ? readConfigurationFile(file) : null;
    }

    private File locateConfigFile(String fileName) {
        URL fileUrl = getClass().getResource(fileName);
        if (fileUrl == null) {
            log.debug(String.format("File resource [%s] can't be found by this class classloader. We'll try with SystemClassLoader...", fileName));

            fileUrl = ClassLoader.getSystemResource(fileName);
            if (fileUrl == null) {
                log.debug(String.format("File resource [%s] can't be found by SystemClassLoader.", fileName));
            }
        }

        File file = null;
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

        return file;
    }

    /**
     * Reads configuration form a single YAML file.
     * Properties values rewriting takes place using Java System properties with keys prefixed with "apiml."
     *
     * @param file
     * @return
     */
    public ApiMediationServiceConfig readConfigurationFile(File file) {
        ApiMediationServiceConfig configuration = null;

        if (file != null) {
            try {
                String data = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                StringBuilder configFileDataBuilder = resolveExpressions(data, ApiMediationServiceConfigReader.getApiMlServiceContext());
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                configuration = mapper.readValue(configFileDataBuilder.toString(), ApiMediationServiceConfig.class);
            } catch (FileNotFoundException | NoSuchFileException e) {
                throw new ApiMediationServiceConfigReaderException(String.format("File [%s] doesn't exist", file.getName()));
            } catch (IOException e) {
                throw new ApiMediationServiceConfigReaderException(String.format("File [%s] can't be parsed as ApiMediationServiceConfig", file.getName()));
            }
        }

        return configuration;
    }

    /**
     * Utility method for setting Java System properties form ServletContext parameters who's keys are prefixed with "apiml."
     *
     * @param servletContext
     */
    public static void setApiMlServiceContext(ServletContext servletContext) {
        try {
            Context initialContext = new InitialContext();
            Context envContext = (Context)initialContext.lookup("java:comp/env");
            Enumeration<String> paramNames = servletContext.getInitParameterNames();
            while (paramNames.hasMoreElements()) {
                String param = paramNames.nextElement();
                String value = servletContext.getInitParameter(param);
                if (param.startsWith("apiml.")) {
                    envContext.addToEnvironment(param, value);
                }
            }
        } catch (NamingException e) {
            log.error("Couldn't create initialContext", e);
        }
    }

    /**
     * Returns a map with parameters previously set in the JNDI java:comp/env context environment.
     * Olny parameters with keys starting with "apiml." are filtered in.
     * @return
     */
    private static Map<String, String> getApiMlServiceContext() {
        Hashtable<String, String> params = null;
        try {
            Context initialContext = new InitialContext();
            Context apimlContext = (Context)initialContext.lookup("java:comp/env");

            params = (Hashtable)apimlContext.getEnvironment();
        } catch (NamingException e) {
            log.error("Exeption caught while getting java:comp/env JND environment parameters", e);
        }

        if (params != null) {
            Map<String, String> propertiesMap = new HashMap();
            Set<String> keys = params.keySet();
            for (String key : keys) {
                if (key.startsWith("apiml.")) {
                    String value = params.get(key);
                    propertiesMap.put(key, value);
                }
            }
            return propertiesMap;
        }
        return null;
    }

    public static String getApiMlServiceContextParameter(String paramName) {
        return  getApiMlServiceContext().get(paramName);
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
    public ApiMediationServiceConfig initializeAPIMLConfiguration(ServletContext context) {

        /*
         * Set Java system properties from ServletContext parameters starting with "apiml." prefix.
         * If rewriting is not used, it is not necessary to call {@link ApiMediationServiceConfigReader#setSystemProperties}
         */
        ApiMediationServiceConfigReader.setApiMlServiceContext(context);

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

    public static void setInstanceIpAddress(ApiMediationServiceConfig apimlConfig) {
        //String preferIpAddress = getApiMlServiceContextParameter("apiml.config.eureka.instance.preferIpAddress");
        if ((apimlConfig.getServiceIpAddress() == null) /*|| Boolean.TRUE.equals(Boolean.valueOf(preferIpAddress))*/) {
            URL baseUrl = null;
            try {
                baseUrl = new URL(apimlConfig.getBaseUrl());
            } catch (MalformedURLException e) {
                log.error("" + e);
            }
            String hostname = baseUrl.getHost();

            String ipAddress = UrlUtils.getHostFirstIPAddress(hostname);
            apimlConfig.setServiceIpAddress(ipAddress);
        }
    }
}
