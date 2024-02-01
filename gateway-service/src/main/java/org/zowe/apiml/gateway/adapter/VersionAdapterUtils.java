/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.adapter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@UtilityClass
public class VersionAdapterUtils {

    private static Adapter<jakarta.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletRequest>
        HTTP_SERVLET_REQUEST_JAKARTA_JAVAX = new Adapter<>(
            jakarta.servlet.http.HttpServletRequest.class,
            javax.servlet.http.HttpServletRequest.class
        );

    private static Adapter<javax.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletRequest>
        HTTP_SERVLET_REQUEST_JAVAX_JAKARTA = new Adapter<>(
            javax.servlet.http.HttpServletRequest.class,
            jakarta.servlet.http.HttpServletRequest.class
        );

    private static Adapter<jakarta.servlet.http.HttpServletResponse, javax.servlet.http.HttpServletResponse>
        HTTP_SERVLET_RESPONSE_JAKARTA_JAVAX = new Adapter<>(
            jakarta.servlet.http.HttpServletResponse.class,
            javax.servlet.http.HttpServletResponse.class
    );

    private static Adapter<javax.servlet.http.HttpServletResponse, jakarta.servlet.http.HttpServletResponse>
        HTTP_SERVLET_RESPONSE_JAVAX_JAKARTA = new Adapter<>(
            javax.servlet.http.HttpServletResponse.class,
            jakarta.servlet.http.HttpServletResponse.class
    );

    public jakarta.servlet.http.HttpServletRequest toJakarta(javax.servlet.http.HttpServletRequest i) {
        return HTTP_SERVLET_REQUEST_JAVAX_JAKARTA.createProxy(i);
    }

    public javax.servlet.http.HttpServletRequest toJavax(jakarta.servlet.http.HttpServletRequest i) {
        return HTTP_SERVLET_REQUEST_JAKARTA_JAVAX.createProxy(i);
    }

    public jakarta.servlet.http.HttpServletResponse toJakarta(javax.servlet.http.HttpServletResponse i) {
        return HTTP_SERVLET_RESPONSE_JAVAX_JAKARTA.createProxy(i);
    }

    public javax.servlet.http.HttpServletResponse toJavax(jakarta.servlet.http.HttpServletResponse i) {
        return HTTP_SERVLET_RESPONSE_JAKARTA_JAVAX.createProxy(i);
    }

    public jakarta.servlet.http.HttpServlet toJakarta(javax.servlet.http.HttpServlet i) {
        return new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
                try {
                    i.service(toJavax((HttpServletRequest) req), toJavax((HttpServletResponse) res));
                } catch (javax.servlet.ServletException se) {
                    throw new ServletException(se);
                }
            }
        };
    }

    public javax.servlet.FilterChain toJavax(jakarta.servlet.FilterChain i) {
        return (r, s) -> {
            try {
                i.doFilter(toJakarta((javax.servlet.http.HttpServletRequest) r), toJakarta((javax.servlet.http.HttpServletResponse) s));
            } catch (jakarta.servlet.ServletException se) {
                throw new javax.servlet.ServletException(se);
            }
        };
    }

    public jakarta.servlet.http.HttpFilter toJakarta(javax.servlet.Filter i) {
        return new jakarta.servlet.http.HttpFilter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                try {
                    i.doFilter(
                        toJavax((HttpServletRequest) request),
                        toJavax((HttpServletResponse) response),
                        toJavax(chain)
                    );
                } catch (javax.servlet.ServletException e) {
                    throw new jakarta.servlet.ServletException(e);
                }
            }
        };
    }

    public org.apache.http.impl.client.CloseableHttpClient httpClient4(org.apache.hc.client5.http.impl.classic.CloseableHttpClient i) {
        final Adapter<org.apache.hc.core5.http.ClassicHttpResponse, org.apache.http.client.methods.CloseableHttpResponse> RESPONSE_ADAPTER =
            new Adapter<>(org.apache.hc.core5.http.ClassicHttpResponse.class, org.apache.http.client.methods.CloseableHttpResponse.class);
        final Adapter<org.apache.http.HttpRequest, org.apache.hc.core5.http.ClassicHttpRequest> REQUEST_ADAPTER =
            new Adapter<>(org.apache.http.HttpRequest.class, org.apache.hc.core5.http.ClassicHttpRequest.class);
        final Adapter<org.apache.http.protocol.HttpContext, org.apache.hc.core5.http.protocol.HttpContext> CONTEXT_ADAPTER =
            new Adapter<>(org.apache.http.protocol.HttpContext.class, org.apache.hc.core5.http.protocol.HttpContext.class);

        return new org.apache.http.impl.client.CloseableHttpClient() {
            @Override
            public HttpParams getParams() {
                throw new RuntimeException("Unsupported method by adapter");
            }

            @Override
            public ClientConnectionManager getConnectionManager() {
                throw new RuntimeException("Unsupported method by adapter");
            }

            @Override
            public void close() throws IOException {
                i.close();
            }

            @Override
            protected org.apache.http.client.methods.CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
                org.apache.hc.core5.http.HttpHost target5 = new org.apache.hc.core5.http.HttpHost(target.getSchemeName(), target.getAddress(), target.getHostName(), target.getPort());
                org.apache.hc.core5.http.ClassicHttpRequest request5 = REQUEST_ADAPTER.createProxy(request);
                org.apache.hc.core5.http.protocol.HttpContext context5 = CONTEXT_ADAPTER.createProxy(context);

                org.apache.hc.client5.http.impl.classic.CloseableHttpResponse response5 = i.execute(target5, request5, context5);
                return RESPONSE_ADAPTER.createProxy(response5);
            }
        };
    }

    class Adapter<A, B> {

        private Class<A> classA;
        private Class<B> classB;

        private Map<Method, MethodInvoker> handlers = new HashMap<>();

        Adapter(Class<A> classA, Class<B> classB) {
            this.classA = classA;
            this.classB = classB;
            init();
        }

        private static String getMethodKey(Method method) {
            StringBuilder sb = new StringBuilder();
            sb.append(method.getName());
            sb.append('(');

            Class<?>[] args = method.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(args[i].getSimpleName());
            }

            sb.append(')');
            return sb.toString();
        }

        private void init() {
            Map<String, Method> aMethods = new HashMap<>();

            for (Method a : classA.getDeclaredMethods()) {
                aMethods.put(getMethodKey(a), a);
            }
            for (Method b : classB.getDeclaredMethods()) {
                Method a = aMethods.remove(getMethodKey(b));
                if (a != null) createMapping(a, b);
            }

        }

        private void createMapping(Method a, Method b) {
            try {
                MethodHandle methodHandle = MethodHandles.lookup().unreflect(b);

                Function<Object, Object> responseMapper;
                if (a.getReturnType() == b.getReturnType()) {
                    responseMapper = x -> x;
                } else {
                    if (a.getReturnType().isArray() && b.getReturnType().isArray()) {
                        Class<?> aType = a.getReturnType().getComponentType();
                        Class<?> bType = b.getReturnType().getComponentType();

                        Adapter<Object, Object> typeAdapter = new Adapter(aType, bType);
                        responseMapper = x -> {
                            Object[] aArray = (Object[]) x;
                            Object[] bArray = (Object[]) Array.newInstance(b.getReturnType().arrayType(), aArray.length);
                            for (int i = 0; i < aArray.length; i++) {
                                bArray[i] = typeAdapter.createProxy(aArray[i]);
                            }
                            return bArray;
                        };
                    } else {
                        responseMapper = x -> new Adapter<>((Class<Object>) a.getReturnType(), (Class<Object>) b.getReturnType()).createProxy(x);
                    }
                }

                MethodInvoker mapper = (instance, args) -> {
                    try {
                        return responseMapper.apply(methodHandle.invoke(instance, args));
                    } catch (Throwable t) {
                        throw new RuntimeException("TODO", t);
                    }
                };

                this.handlers.put(a, mapper);
            } catch (IllegalAccessException iae) {
                // skip protect / private method
                //throw new IllegalStateException("Cannot generate handlers of proxy, please verify dependencies and the compatibility of classes", iae);
            }
        }

        public B createProxy(A i) {
            return (B) Proxy.newProxyInstance(
                VersionAdapterUtils.class.getClassLoader(),
                new Class[] { classB },
                (proxy, method, methodArgs) -> handlers.get(method).method(i, methodArgs)
            );
        }


    }

    interface MethodInvoker {

        Object method(Object instance, Object[] args) throws Exception;

    }

    public static void main(String[] args) throws IllegalAccessException {
        new Adapter<>(jakarta.servlet.http.HttpServletRequest.class, javax.servlet.http.HttpServletRequest.class).createProxy(null);
    }


}
