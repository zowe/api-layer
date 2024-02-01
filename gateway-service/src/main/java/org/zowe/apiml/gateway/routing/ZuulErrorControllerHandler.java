/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.routing;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.*;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.config.ZuulConfigurationOverriden.ZuulController;
import org.zowe.apiml.product.compatibility.ApimlErrorController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to reconcile the breaking change between Spring Boot 2.5 and Zuul. The breaking change
 * is due to ErrorController.getErrorPath being removed in Spring Boot 2.5. A BeanPostProcessor is used
 * to proxy ZuulHandlerMapping, intercepting the code execution that leads to the NoSuchMethodError.
 * <p>
 * NOTE: This should be removed when the APIML migrates away from Zuul to Spring Cloud Gateway.
 */
@Component
public class ZuulErrorControllerHandler implements BeanPostProcessor {
    private final RouteLocator routeLocator;
    private final ZuulController zuulController;
    private final List<ApimlErrorController> errorControllers;

    public ZuulErrorControllerHandler(RouteLocator routeLocator, ZuulController zuulController, List<ApimlErrorController> errorControllers) {
        this.routeLocator = routeLocator;
        this.zuulController = zuulController;
        this.errorControllers = errorControllers;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        boolean hasErrorController = errorControllers != null && !errorControllers.isEmpty();
        if (hasErrorController && (bean instanceof ZuulHandlerMapping)) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(ZuulHandlerMapping.class);
            enhancer.setCallbackFilter(LookupHandlerCallbackFilter.INSTANCE);
            enhancer.setCallbacks(new Callback[]{new LookupHandlerMethodInterceptor(errorControllers), NoOp.INSTANCE});
            Constructor<?> ctor = ZuulHandlerMapping.class.getConstructors()[0];
            return enhancer.create(ctor.getParameterTypes(), new Object[]{routeLocator, zuulController});
        }
        return bean;
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
        private final List<String> errorPaths = new ArrayList<>();

        LookupHandlerMethodInterceptor(List<ApimlErrorController> errorControllers) {
            for (ApimlErrorController errorController : errorControllers) {
                errorPaths.add(errorController.getErrorPath());
            }
        }

        @Override
        public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            if (args[0] instanceof String && errorPaths.contains(args[0])) {
                return null;
            }
            return methodProxy.invokeSuper(target, args);
        }
    }
}
