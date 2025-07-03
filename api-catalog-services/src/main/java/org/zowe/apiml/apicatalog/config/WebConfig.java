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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;
import java.time.Duration;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

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

    @Bean
    public RouterFunction<ServerResponse> redirectRoot() {
        return route(GET("/"), req ->
            ServerResponse.temporaryRedirect(URI.create("/apicatalog"))
                .build());
    }

    @Bean
    public RouterFunction<ServerResponse> redirectApicatalogRoot() {
        return route(GET("/apicatalog"), req ->
            ServerResponse.temporaryRedirect(URI.create("/apicatalog/index.html"))
                .build());
    }

    @Bean
    public RouterFunction<ServerResponse> redirectApicatalogVersionRoot() {
        return route(GET("/apicatalog/api/v1"), req ->
            ServerResponse.temporaryRedirect(URI.create("/apicatalog/api/v1/index.html"))
                .build());
    }

}
