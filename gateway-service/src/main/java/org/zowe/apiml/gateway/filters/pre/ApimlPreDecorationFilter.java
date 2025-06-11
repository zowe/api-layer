/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.pre;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import javax.annotation.PostConstruct;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

@Component
@Primary
//TODO: check with PJ
//@ConditionalOnMissingBean(PreDecorationFilter.class)
public class ApimlPreDecorationFilter extends PreDecorationFilter {

    // Generic all-in-one Forwarded header not handled by the default filter
    public static final String FORWARDED_HEADER = "Forwarded";

    private static final Log log = LogFactory.getLog(ApimlPreDecorationFilter.class);

    private static final String ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE = "javax.servlet.request.X509Certificate";

    @Value("${apiml.security.forwardHeader.trusted-proxies:#{null}}")
    private String trustedProxies;

    private final CertificateValidator certificateValidator;

    private Predicate<String> isHostTrusted = host -> false;

    public ApimlPreDecorationFilter(
        RouteLocator routeLocator, ProxyRequestHelper proxyRequestHelper,
        ZuulProperties zuulProperties, ServerProperties server,
        CertificateValidator certificateValidator
    ) {
        super(routeLocator, server.getServlet().getContextPath(), zuulProperties, proxyRequestHelper);
        this.certificateValidator = certificateValidator;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        if (trustedProxies != null) {
            Pattern pattern = Pattern.compile(trustedProxies);
            isHostTrusted = host -> pattern.matcher(host).matches();
        }
    }

    private boolean isTrusted(RequestContext ctx) {
        X509Certificate[] certs = (X509Certificate[]) ctx.getRequest().getAttribute(ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE);
        if (certificateValidator.isTrusted(certs)) return true;

        return isHostTrusted.test(ctx.getRequest().getRemoteAddr());
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        boolean isProxyTrusted = isTrusted(ctx);

        if (!isProxyTrusted) {
            // when the request is not from a trusted proxy, remove the headers and untrusted remote address
            ctx.setRequest(
                new HttpServletRequestWrapper(ctx.getRequest()) {
                    @Override
                    public String getRemoteAddr() { return null; }

                    @Override
                    public long getDateHeader(String name) {
                        if (isXForwardedHeader(name)) {
                            return -1;
                        } else return super.getDateHeader(name);
                    }

                    @Override
                    public String getHeader(String name) {
                        if (isXForwardedHeader(name)) {
                            return null;
                        } else return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if (isXForwardedHeader(name)) {
                            return Collections.emptyEnumeration();
                        } else return super.getHeaders(name);
                    }

                    @Override
                    public Enumeration<String> getHeaderNames() {
                        List<String> namesList = Collections.list(super.getHeaderNames());
                        namesList.removeIf(this::isXForwardedHeader);
                        return Collections.enumeration(namesList);
                    }

                    @Override
                    public int getIntHeader(String name) {
                        if (isXForwardedHeader(name)) {
                            return -1;
                        } else return super.getIntHeader(name);
                    }

                    private boolean isXForwardedHeader(String header) {
                        return header.equalsIgnoreCase(X_FORWARDED_FOR_HEADER) ||
                            header.equalsIgnoreCase(X_FORWARDED_HOST_HEADER) ||
                            header.equalsIgnoreCase(X_FORWARDED_PORT_HEADER) ||
                            header.equalsIgnoreCase(X_FORWARDED_PROTO_HEADER) ||
                            header.equalsIgnoreCase(X_FORWARDED_PREFIX_HEADER) ||
                            header.equalsIgnoreCase(FORWARDED_HEADER);
                    }
                }
            );
        }
        return super.run();
    }
}
