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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.*;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulController;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.product.compatibility.ApimlErrorController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@Configuration
public class ZuulConfig {

    @Bean
    public ZuulPostProcessor zuulPostProcessor(@Autowired RouteLocator routeLocator, @Autowired ZuulController zuulController,
                                               @Autowired(required = false) ApimlErrorController errorController) {
        return new ZuulPostProcessor(routeLocator, zuulController, errorController);
    }

    /**
     * This class is used to reconcile the breaking change between Spring Boot 2.5 and Zuul. The breaking change
     * is due to ErrorController.getErrorPath being removed in Spring Boot 2.5. A BeanPostProcessor is used
     * to proxy ZuulHandlerMapping, intercepting the code execution that leads to the NoSuchMethodError.
     * <p>
     * NOTE: This should be removed when the APIML migrates away from Zuul to Spring Cloud Gateway.
     */
    private static class ZuulPostProcessor implements BeanPostProcessor {
        private final RouteLocator routeLocator;
        private final ZuulController zuulController;
        private final ApimlErrorController errorController;

        ZuulPostProcessor(RouteLocator routeLocator, ZuulController zuulController, ApimlErrorController errorController) {
            this.routeLocator = routeLocator;
            this.zuulController = zuulController;
            this.errorController = errorController;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (errorController != null && (bean instanceof ZuulHandlerMapping)) {
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(ZuulHandlerMapping.class);
                enhancer.setCallbackFilter(LookupHandlerCallbackFilter.INSTANCE);
                enhancer.setCallbacks(new Callback[]{new LookupHandlerMethodInterceptor(errorController.getErrorPath()), NoOp.INSTANCE});
                Constructor<?> ctor = ZuulHandlerMapping.class.getConstructors()[0];
                return enhancer.create(ctor.getParameterTypes(), new Object[]{routeLocator, zuulController});
            }
            return bean;
        }
    }

    private enum LookupHandlerCallbackFilter implements CallbackFilter {
        INSTANCE;

        @Override
        public int accept(Method method) {
            if ("lookupHandler".equals(method.getName())) {
                return 0;
            }
            return 1;
        }
    }

    private static final class LookupHandlerMethodInterceptor implements MethodInterceptor {
        private final String errorPath;

        LookupHandlerMethodInterceptor(String errorPath) {
            this.errorPath = errorPath;
        }

        @Override
        public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            if (errorPath.equals(args[0])) {
                return null;
            }
            return methodProxy.invokeSuper(target, args);
        }
    }
}
