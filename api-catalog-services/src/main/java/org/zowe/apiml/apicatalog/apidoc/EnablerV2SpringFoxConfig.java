/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.apidoc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.apicatalog.apidoc.model.ApiDocConfigException;
import org.zowe.apiml.apicatalog.apidoc.model.ApiPropertiesContainer;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

import static springfox.documentation.builders.PathSelectors.regex;

@SuppressWarnings("Duplicates")
@Slf4j
@Configuration
@EnableSwagger2
public class EnablerV2SpringFoxConfig {

    @Autowired
    public EnablerV2SpringFoxConfig(ApiPropertiesContainer apiPropertiesContainer,
                                    DefaultListableBeanFactory beanFactory) {
        apiPropertiesContainer.getApiVersionProperties().forEach((apiVersion, apiInfo) -> {
            Docket apiDocketBean;
            try {
                apiDocketBean = generateDocket(apiVersion, apiInfo);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return;
            }

            // create a bean name based on the group
            String beanName = apiVersion.trim() + "_API";
            beanFactory.initializeBean(apiDocketBean, beanName);
            beanFactory.autowireBeanProperties(apiDocketBean,
                AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
            beanFactory.registerSingleton(beanName, apiDocketBean);
            log.info("Generated bean: " + beanName + " for API version: " + apiVersion.trim());
        });
    }

    /**
     * Generate a docket for this API version
     *
     * @param apiVersion the version of the API
     * @param apiInfo the API Info
     * @return a SpringFox docket
     * @throws ApiDocConfigException in case of error
     */
    private Docket generateDocket(String apiVersion, ApiPropertiesContainer.ApiProperties apiInfo) {
        String groupName = apiVersion;
        if (apiInfo.getGroupName() != null && !apiInfo.getGroupName().isEmpty()) {
            groupName = apiInfo.getGroupName();
        }
        if (apiInfo.getBasePackage() == null && apiInfo.getApiPattern() == null) {
            return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .build()
                .apiInfo(
                    new ApiInfo(apiInfo.getTitle(),
                        apiInfo.getDescription(),
                        apiInfo.getVersion(),
                       null,
                        null,
                        null,
                        null,
                        Collections.emptyList()))
                .groupName(groupName);
        } else if (apiInfo.getBasePackage() != null && !apiInfo.getBasePackage().isEmpty()) {
            return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage(apiInfo.getBasePackage()))
                .build()
                .apiInfo(
                    new ApiInfo(apiInfo.getTitle(),
                        apiInfo.getDescription(),
                        apiInfo.getVersion(),
                        null,
                        null,
                        null,
                        null,
                        Collections.emptyList()))
                .groupName(groupName);
        } else if (apiInfo.getApiPattern() != null && !apiInfo.getApiPattern().isEmpty()) {
            return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .paths(regex(apiInfo.getApiPattern()))
                .build()
                .apiInfo(
                    new ApiInfo(apiInfo.getTitle(),
                        apiInfo.getDescription(),
                        apiInfo.getVersion(),
                        null,
                        null,
                        null,
                        null,
                        Collections.emptyList()))
                .groupName(groupName);
        } else {
            String msg = "A base package or API pattern was not found for version: " + apiVersion;
            ApiDocConfigException t = new ApiDocConfigException(msg);
            log.error(msg, t);
            throw t;
        }
    }
}
