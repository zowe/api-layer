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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


public class ApiMediationServiceConfigReader {
    private static final Logger logger = LoggerFactory.getLogger(ApiMediationServiceConfigReader.class);

    public ApiMediationServiceConfigReader() {
    }

    public ApiMediationServiceConfig mergeConfigurations(ApiMediationServiceConfig defaultConfiguration, String additionalConfigFile/*, int mergeFlags*/) {
        return mergeConfigurations(defaultConfiguration, readConfigurationFile(additionalConfigFile)/*, int mergeFlags*/);
    }

    public ApiMediationServiceConfig mergeConfigurations(ApiMediationServiceConfig defaultConfiguration, ApiMediationServiceConfig  additionalConfiguration /*, int mergeFlags*/) {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> defaultConfigPropertiesMap = mapper.convertValue(defaultConfiguration, Map.class);
        Map<String, Object> additionalConfigPropertiesMap = mapper.convertValue(additionalConfiguration, Map.class);

        if (additionalConfigPropertiesMap != null) {
            defaultConfigPropertiesMap.putAll(additionalConfigPropertiesMap);
        }

        //replaceValuesFromSystemProperties(defaultConfigPropertiesMap);

        return mapper.convertValue(defaultConfigPropertiesMap, ApiMediationServiceConfig.class);
    }

    public static void setSystemProperties(ServletContext context) {
        Enumeration<String> paramNames = context.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String param = paramNames.nextElement();
            String value = context.getInitParameter(param);
            if (param.startsWith("apiml.")) {
                System.setProperty(param, value);
            }
        }
    }

    private Map<String, String> getApiMlSystemProperties() {
        Map<String, String>  propertiesMap = new HashMap();

        Properties systemProperties = System.getProperties();
        Set<String> systemPropsMapKeys = systemProperties.stringPropertyNames();
        for (Iterator<String> it = systemPropsMapKeys.iterator(); it.hasNext(); ) {
            String key = it.next();
            if (key.startsWith("apiml.")) {
                Object propVal = systemProperties.get(key);
                String sysValue = (propVal != null) ? propVal.toString() : null;
                    propertiesMap.put(key, sysValue);
            }
        }

        return propertiesMap;
    }

    public ApiMediationServiceConfig loadConfiguration(String defaultConfigurationFileName, String externalizedConfigurationFileName) {
        ApiMediationServiceConfigReader configReader = new ApiMediationServiceConfigReader();
        ApiMediationServiceConfig serviceConfig = configReader.readConfigurationFile(defaultConfigurationFileName);

        if (externalizedConfigurationFileName != null) {
            serviceConfig = configReader.mergeConfigurations(serviceConfig, externalizedConfigurationFileName/*, ApiMediationServiceConfigReader.MERGE_FLAG_THROW_IF_MISSING*/);
        }

        return serviceConfig;
    }

    public ApiMediationServiceConfig readConfigurationFile(String fileName) {

        File file = locateConfigFile(fileName);

        return (file != null) ? readConfigurationFile(file) : null;
    }

    private File locateConfigFile(String fileName) {
        /*
         *  First if the file path is not absolute, try to locate the config as a resource using class loaders
         */
        URL fileUrl = getClass().getResource(fileName);
        if (fileUrl == null) {
            logger.warn(String.format("File resource [%s] can't be found by this class classloader. We'll try with SystemClassLoader...", fileName));

            fileUrl = ClassLoader.getSystemResource(fileName);
            if (fileUrl == null) {
                logger.warn(String.format("File resource [%s] can't be found by SystemClassLoader.", fileName));
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

    private ApiMediationServiceConfig readConfigurationFile(File file) {
        ApiMediationServiceConfig configuration = null;

        if (file != null) {
            try {
                //Scanner sc = new Scanner(file);
                //sc.useDelimiter("\\Z");
                //String fileContents =  sc.next();
                //System.out.println(fileContents);

                String data = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                //System.out.println(data);
                StringBuilder configFileDataBuilder = resolveExpressions(data, getApiMlSystemProperties());
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());//.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                configuration = mapper.readValue(configFileDataBuilder.toString(), ApiMediationServiceConfig.class);
                //System.out.println(configuration.toString());
            } catch (FileNotFoundException | NoSuchFileException e) {
                throw new ApiMediationServiceConfigReaderException(String.format("File [%s] doesn't exist", file.getName()));
            } catch (IOException e) {
                throw new ApiMediationServiceConfigReaderException(String.format("File [%s] can't be parsed as ApiMediationServiceConfig", file.getName()));
            }
        }

        return configuration;
    }

    static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]*)\\}");

    static StringBuilder resolveExpressions(String expression, Map<String, String> properties) {
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
}
