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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.gateway.services.ServiceInstancesUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;

/**
 * Extracts the client certificate from the TLS handshake and forwards it in the {@code Client-Cert} request header
 * to the downstream service. Only active when {@code apiml.service.forwardClientCertEnabled=true} is configured
 * and the routed service declares {@value EurekaMetadataDefinition#SERVICE_SUPPORTING_CLIENT_CERT_FORWARDING}
 * in its Eureka metadata.
 *
 * <p>Any pre-existing {@code Client-Cert} header is always removed to prevent spoofing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientCertFilter extends PreZuulFilter {

    public static final String CLIENT_CERT_HEADER = "Client-Cert";
    private static final String CLIENT_CERT_ATTRIBUTE = "client.auth.X509Certificate";

    @Value("${apiml.service.forwardClientCertEnabled:false}")
    private boolean forwardingClientCertEnabled;

    private final DiscoveryClient discoveryClient;

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 8;
    }

    @Override
    public boolean shouldFilter() {
        if (!forwardingClientCertEnabled) {
            return false;
        }
        List<ServiceInstance> instances = ServiceInstancesUtils.getServiceInstancesFromDiscoveryClient(discoveryClient);
        if (instances == null || instances.isEmpty()) {
            return false;
        }
        String value = instances.get(0).getMetadata().get(EurekaMetadataDefinition.SERVICE_SUPPORTING_CLIENT_CERT_FORWARDING);
        return Boolean.parseBoolean(value);
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        context.addZuulRequestHeader(CLIENT_CERT_HEADER, null);

        HttpServletRequest request = context.getRequest();

        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(CLIENT_CERT_ATTRIBUTE);
        if (certs == null || certs.length == 0) {
            certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        }

        if (certs != null && certs.length > 0) {
            try {
                String encodedCert = Base64.getEncoder().encodeToString(certs[0].getEncoded());
                context.addZuulRequestHeader(CLIENT_CERT_HEADER, encodedCert);
                log.debug("Incoming client certificate has been added to the {} header.", CLIENT_CERT_HEADER);
            } catch (CertificateEncodingException e) {
                log.debug("Failed to encode the incoming client certificate. Error message: {}", e.getMessage());
                context.addZuulRequestHeader(ApimlConstants.AUTH_FAIL_HEADER, "Invalid client certificate in request. Error message: " + e.getMessage());
            }
        }

        return null;
    }
}
