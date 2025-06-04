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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.pre.InsecureRequestPathException;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.cert.X509Certificate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

@Component
@Primary
@ConditionalOnMissingBean(PreDecorationFilter.class)
public class ApimlPreDecorationFilter extends PreDecorationFilter {

    private static final Log log = LogFactory.getLog(ApimlPreDecorationFilter.class);

    private static final String ATTRNAME_JAVAX_SERVLET_REQUEST_X509_CERTIFICATE = "javax.servlet.request.X509Certificate";

    @Value("${apiml.security.forwardHeader.trusted-proxies:#{null}}")
    private String trustedProxies;

    private final RouteLocator routeLocator;
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();
    private final CertificateValidator certificateValidator;

    private Predicate<String> isHostTrusted = host -> false;

    private final boolean addProxyHeaders;

    public ApimlPreDecorationFilter(
        RouteLocator routeLocator, ProxyRequestHelper proxyRequestHelper,
        ZuulProperties zuulProperties, ServerProperties server,
        CertificateValidator certificateValidator
    ) {
        super(routeLocator, server.getServlet().getContextPath(), zuulProperties, proxyRequestHelper);
        this.routeLocator = routeLocator;
        this.certificateValidator = certificateValidator;

        this.urlPathHelper.setRemoveSemicolonContent(zuulProperties.isRemoveSemicolonContent());
        this.urlPathHelper.setUrlDecode(zuulProperties.isDecodeUrl());

        // keep original configuration
        this.addProxyHeaders = zuulProperties.isAddProxyHeaders();

        // to disable original source code
        zuulProperties.setAddProxyHeaders(false);
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
        final String requestURI = this.urlPathHelper
            .getPathWithinApplication(ctx.getRequest());
        if (insecurePath(requestURI)) {
            throw new InsecureRequestPathException(requestURI);
        }
        Route route = this.routeLocator.getMatchingRoute(requestURI);

        boolean isProxyTrusted = isTrusted(ctx);

        if (!isProxyTrusted) {
            // when the request is not from a trusted proxy remove the headers
            ctx.addZuulRequestHeader(X_FORWARDED_FOR_HEADER, null);
        }

        // decorate the request with original code (see disable feature via addProxyHeaders)
        Object filterResponse = super.run();

        if (addProxyHeaders && isProxyTrusted) {
            // if the proxy is trusted call the same code as in the original source (skipped one)
            addProxyHeaders(ctx, route);
            String xforwardedfor = ctx.getRequest()
                .getHeader(X_FORWARDED_FOR_HEADER);
            String remoteAddr = ctx.getRequest().getRemoteAddr();
            if (xforwardedfor == null) {
                xforwardedfor = remoteAddr;
            }
            else if (!xforwardedfor.contains(remoteAddr)) { // Prevent duplicates
                xforwardedfor += ", " + remoteAddr;
            }
            ctx.addZuulRequestHeader(X_FORWARDED_FOR_HEADER, xforwardedfor);
        }

        return filterResponse;
    }

    // copy of the original accessible code

    private boolean insecurePath(String path) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        if (path.contains("%")) {
            try {
                path = URLDecoder.decode(path, "UTF-8");
            }
            catch (UnsupportedEncodingException ignored) {
                // Should never happen...
            }
        }
        if (isInsecurePath(path)) {
            return true;
        }
        return isInsecurePath(urlPathHelper.removeSemicolonContent(path));
    }

    private boolean isInsecurePath(String path) {
        if (path.contains(":/")) {
            String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
            if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
                if (log.isWarnEnabled()) {
                    log.warn(
                        "Path represents URL or has \"url:\" prefix: [" + path + "]");
                }
                return true;
            }
        }
        if (path.contains("../")) {
            if (log.isWarnEnabled()) {
                log.warn("Path contains \"../\"");
            }
            return true;
        }
        if (path.contains("..\\")) {
            if (log.isWarnEnabled()) {
                log.warn("Path contains \"..\\\"");
            }
            return true;
        }
        return false;
    }

    private void addProxyHeaders(RequestContext ctx, Route route) {
        HttpServletRequest request = ctx.getRequest();
        String host = toHostHeader(request);
        String port = String.valueOf(request.getServerPort());
        String proto = request.getScheme();
        if (hasHeader(request, X_FORWARDED_HOST_HEADER)) {
            host = request.getHeader(X_FORWARDED_HOST_HEADER) + "," + host;
        }
        if (!hasHeader(request, X_FORWARDED_PORT_HEADER)) {
            if (hasHeader(request, X_FORWARDED_PROTO_HEADER)) {
                StringBuilder builder = new StringBuilder();
                for (String previous : StringUtils.commaDelimitedListToStringArray(
                    request.getHeader(X_FORWARDED_PROTO_HEADER))) {
                    if (builder.length() > 0) {
                        builder.append(",");
                    }
                    builder.append(
                        HTTPS_SCHEME.equals(previous) ? HTTPS_PORT : HTTP_PORT);
                }
                builder.append(",").append(port);
                port = builder.toString();
            }
        }
        else {
            port = request.getHeader(X_FORWARDED_PORT_HEADER) + "," + port;
        }
        if (hasHeader(request, X_FORWARDED_PROTO_HEADER)) {
            proto = request.getHeader(X_FORWARDED_PROTO_HEADER) + "," + proto;
        }
        ctx.addZuulRequestHeader(X_FORWARDED_HOST_HEADER, host);
        ctx.addZuulRequestHeader(X_FORWARDED_PORT_HEADER, port);
        ctx.addZuulRequestHeader(X_FORWARDED_PROTO_HEADER, proto);
        addProxyPrefix(ctx, route);
    }

    private String toHostHeader(HttpServletRequest request) {
        int port = request.getServerPort();
        if ((port == HTTP_PORT && HTTP_SCHEME.equals(request.getScheme()))
            || (port == HTTPS_PORT && HTTPS_SCHEME.equals(request.getScheme()))) {
            return request.getServerName();
        }
        else {
            return request.getServerName() + ":" + port;
        }
    }

    private boolean hasHeader(HttpServletRequest request, String name) {
        return StringUtils.hasLength(request.getHeader(name));
    }

    private void addProxyPrefix(RequestContext ctx, Route route) {
        String forwardedPrefix = ctx.getRequest().getHeader(X_FORWARDED_PREFIX_HEADER);
        String contextPath = ctx.getRequest().getContextPath();
        String prefix = StringUtils.hasLength(forwardedPrefix) ? forwardedPrefix
            : (StringUtils.hasLength(contextPath) ? contextPath : null);
        if (StringUtils.hasText(route.getPrefix())) {
            StringBuilder newPrefixBuilder = new StringBuilder();
            if (prefix != null) {
                if (prefix.endsWith("/") && route.getPrefix().startsWith("/")) {
                    newPrefixBuilder.append(prefix, 0, prefix.length() - 1);
                }
                else {
                    newPrefixBuilder.append(prefix);
                }
            }
            newPrefixBuilder.append(route.getPrefix());
            prefix = newPrefixBuilder.toString();
        }
        if (prefix != null) {
            ctx.addZuulRequestHeader(X_FORWARDED_PREFIX_HEADER, prefix);
        }
    }

}
