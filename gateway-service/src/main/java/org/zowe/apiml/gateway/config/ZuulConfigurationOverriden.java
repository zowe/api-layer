/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.filters.ZuulServletFilter;
import com.netflix.zuul.http.ZuulServlet;
import com.netflix.zuul.monitoring.CounterFactory;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.metrics.DefaultCounterFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ServletWrappingController;
import org.zowe.apiml.gateway.adapter.VersionAdapterUtils;

import java.util.Collections;

@Configuration
public class ZuulConfigurationOverriden {

    @Bean
    public ZuulProperties zuulProperties() {
        return new ZuulProperties();
    }

    @Bean
    public ZuulController zuulController() {
        return new ZuulController();
    }

    @Bean
    public CounterFactory counterFactory(MeterRegistry meterRegistry) {
        return new DefaultCounterFactory(meterRegistry);
    }

    @Bean
    public FilterRegistrationBean zuulServlet(ZuulProperties zuulProperties) {
        final FilterRegistrationBean<Filter> filterRegistration = new FilterRegistrationBean<>();
        filterRegistration.setUrlPatterns(
                Collections.singleton(zuulProperties.getServletPattern()));
        filterRegistration.setFilter(VersionAdapterUtils.toJakarta(new ZuulServletFilter()));
        filterRegistration.setOrder(Ordered.LOWEST_PRECEDENCE);
        // The whole point of exposing this servlet is to provide a route that doesn't
        // buffer requests.
        filterRegistration.addInitParameter("buffer-requests", "false");
        return filterRegistration;
    }

    public class ZuulController extends  ServletWrappingController {

        private HttpServlet zuulServlet;

        public ZuulController() {
            zuulServlet = VersionAdapterUtils.toJakarta(new ZuulServlet());

            //setServletClass(ZuulServlet.class);
            setServletName("zuul");
            setSupportedMethods((String[]) null); // Allow all
        }

        @Override
        public void afterPropertiesSet() throws Exception {
        }

        @Override
        protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
            return super.handleRequestInternal(request, response);
        }

        @Override
        public ModelAndView handleRequest(HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            try {
                // We don't care about the other features of the base class, just want to
                // handle the request
                return super.handleRequestInternal(request, response);
            }
            finally {
                // @see com.netflix.zuul.context.ContextLifecycleFilter.doFilter
                RequestContext.getCurrentContext().unset();
            }
        }

    }

}
