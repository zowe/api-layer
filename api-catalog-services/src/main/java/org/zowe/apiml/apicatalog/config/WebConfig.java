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
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
@ComponentScan("org.zowe.apiml.product.web")
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
        .addResourceHandler("/index.html")
        .setCacheControl(CacheControl
            .noStore()
            .cachePrivate()
            .mustRevalidate())
        .addResourceLocations("/static/index.html", "classpath:/static/index.html");

        registry
        .addResourceHandler("/static/**")
        .setCacheControl(CacheControl.maxAge(Duration.ofDays(365L)))
        .addResourceLocations("classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/", "classpath:/static/static/");

        registry
        .addResourceHandler("/resources/**")
        .setCacheControl(CacheControl.maxAge(Duration.ofDays(365L)))
        .addResourceLocations("/resources/**", "/resources/static/**", "/resources/templates/**");
    }
}
