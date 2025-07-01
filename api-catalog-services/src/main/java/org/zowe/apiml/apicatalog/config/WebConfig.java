/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.time.Duration;

@Configuration
@ComponentScan("org.zowe.apiml.product.web")
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
        .addResourceHandler("/apicatalog/*", "/apicatalog/api/ui/*")
        .setCacheControl(CacheControl
            .noStore()
            .cachePrivate()
            .mustRevalidate())
        .addResourceLocations("/static/", "classpath:/static/");

        registry
        .addResourceHandler("/apicatalog/static/**", "/apicatalog/api/ui/static/**")
        .setCacheControl(CacheControl.maxAge(Duration.ofDays(365L)))
        .addResourceLocations("classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/", "classpath:/static/static/");

        registry
        .addResourceHandler("/apicatalog/resources/**", "/apicatalog/api/ui/resources/**")
        .setCacheControl(CacheControl.maxAge(Duration.ofDays(365L)))
        .addResourceLocations("/resources/", "/resources/static/", "/resources/templates/");
    }

}
