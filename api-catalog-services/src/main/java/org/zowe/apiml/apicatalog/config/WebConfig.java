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

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.zowe.apiml.config.ApplicationInfo;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration("catalogWebConfig")
@ComponentScan("org.zowe.apiml.product.web")
@RequiredArgsConstructor
@SuppressWarnings("squid:S1192") // using same literals increase the readability
public class WebConfig implements WebFluxConfigurer {

    private final ApplicationInfo applicationInfo;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = applicationInfo.isModulith() ? "/apicatalog/ui/v1" : "/apicatalog";

        registry
            .addResourceHandler(prefix + "/*")
            .setCacheControl(CacheControl
                .noStore()
                .cachePrivate()
                .mustRevalidate())
            .addResourceLocations("/static/", "classpath:/static/");

        registry
            .addResourceHandler(prefix + "/static/**")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365L)))
            .addResourceLocations("classpath:/META-INF/resources/", "classpath:/resources/", "classpath:/static/", "classpath:/public/", "classpath:/static/static/");

        registry
            .addResourceHandler(prefix + "/resources/**")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365L)))
            .addResourceLocations("/resources/", "/resources/static/", "/resources/templates/");
    }

    private Mono<ServerResponse> redirect(String path) {
        return ServerResponse.permanentRedirect(URI.create(path)).build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "modulithConfig")
    public RouterFunction<ServerResponse> redirectRouteMicroservice() {
        return route(GET("/"), req -> redirect("/apicatalog"))
            .and(route(GET("/apicatalog"), req -> redirect("/apicatalog/")))
            .and(route(GET("/apicatalog/"), req -> redirect("/apicatalog/index.html")));
    }

    @Bean
    @ConditionalOnBean(name = "modulithConfig")
    public RouterFunction<ServerResponse> redirectRouteModulith() {
        return route(GET("/apicatalog/api/v1"), req -> redirect("/apicatalog/api/v1/"))
            .and(route(GET("/apicatalog/api/v1/"), req -> redirect("/apicatalog/api/v1/index.html")))
            .and(route(GET("/apicatalog/ui/v1"), req -> redirect("/apicatalog/ui/v1/")))
            .and(route(GET("/apicatalog/ui/v1/"), req -> redirect("/apicatalog/ui/v1/index.html")))

            .and(route(POST("/apicatalog/api/v1/auth/login"), req -> redirect("/gateway/api/v1/auth/login")))
            .and(route(POST("/apicatalog/api/v1/auth/logout"), req -> redirect("/gateway/api/v1/auth/logout")))
            .and(route(GET("/apicatalog/api/v1/auth/query"), req -> redirect("/gateway/api/v1/auth/query")));
    }

}
