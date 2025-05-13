/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EurekaController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.zowe.apiml.ApimlApplication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ComponentScan(basePackages = "org.zowe.apiml", excludeFilters = {
    @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = ".*Application"
    ),
    @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = EurekaController.class
    )})
@SpringBootTest(classes = {
        ApimlApplication.class,
        FreeMarkerConfigurer.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "server.port=40985" // Use specific port due to need to use of apiml.service.port to determine if it's gateway or DS
    }
)
@AutoConfigureWebTestClient
@DirtiesContext
public @interface AcceptanceTest {

}
