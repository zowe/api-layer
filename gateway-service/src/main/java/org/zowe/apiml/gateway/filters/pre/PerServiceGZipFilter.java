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

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.gzip.GZipResponseUtils;
import org.zowe.apiml.gzip.GZipResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

/**
 * This filter will wrap the response object in GZipResponseWrapper for future compression. Once the response is
 * retrieved from service, it will be written to the GZipOutputStream. It will also add the Content-Encoding header.
 */
@Component
@RequiredArgsConstructor
public class PerServiceGZipFilter extends OncePerRequestFilter {

    private final DiscoveryClient discoveryClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (requiresCompression(request)) {
            final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            final GZIPOutputStream compressedStream = new GZIPOutputStream(compressed);

            final GZipResponseWrapper gzipWrapper = new GZipResponseWrapper(response, compressedStream);
            gzipWrapper.setDisableFlushBuffer(true);
            filterChain.doFilter(request, gzipWrapper);
            gzipWrapper.flush();
            compressedStream.close();
            if (response.isCommitted()) {
                return;
            }

            switch (gzipWrapper.getStatus()) {
                case HttpServletResponse.SC_NO_CONTENT:
                case HttpServletResponse.SC_RESET_CONTENT:
                case HttpServletResponse.SC_NOT_MODIFIED:
                    return;
                default:
            }
            byte[] compressedBytes = compressed.toByteArray();
            boolean shouldGzippedBodyBeZero = GZipResponseUtils.shouldGzippedBodyBeZero(compressedBytes);
            boolean shouldBodyBeZero = GZipResponseUtils.shouldBodyBeZero(gzipWrapper.getStatus());
            if (shouldGzippedBodyBeZero || shouldBodyBeZero) {
                // No reason to add GZIP headers or write body if no content was written or status code specifies no
                // content
                response.setContentLength(0);
                return;
            }

            // Write the zipped body
            GZipResponseUtils.addGzipHeader(response);

            response.setContentLength(compressedBytes.length);

            response.getOutputStream().write(compressedBytes);

        } else {
            filterChain.doFilter(request, response);
        }

    }

    /**
     * The compression is requested when the Client specifies the Accept-Encoding header and there is valid Instance for
     * the service and the Instance specifies that it is interested in compression and either specify no pattern for the
     * and matcher or the URL matches the path.
     *
     * @param request The request to verify
     */
    boolean requiresCompression(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        boolean acceptsCompression = requestAcceptsCompression(request);
        Optional<ServiceInstance> validInstance = getInstanceInfoForUri(requestUri);
        if (!validInstance.isPresent()) {
            return false;
        }
        boolean serviceRequestsCompression = serviceOnRouteRequestsCompression(validInstance.get(), requestUri);

        return acceptsCompression && serviceRequestsCompression;
    }

    // Verify non versioned APIs
    Optional<ServiceInstance> getInstanceInfoForUri(String requestUri) {
        // Compress only if there is valid instance with relevant metadata.
        String[] uriParts = requestUri.split("/");
        List<ServiceInstance> instances;
        if (uriParts.length < 2) {
            return Optional.empty();
        }

        instances = discoveryClient.getInstances(uriParts[1]);
        if (instances == null || instances.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(instances.get(0));
    }

    boolean requestAcceptsCompression(HttpServletRequest request) {
        String encodingHeader = request.getHeader("Accept-Encoding");
        if (encodingHeader == null) {
            return false;
        } else return encodingHeader.contains("gzip");
    }

    boolean serviceOnRouteRequestsCompression(ServiceInstance instance, String requestUri) {
        Map<String, String> metadata = instance.getMetadata();
        boolean allowCompressionForService = "true".equals(metadata.get("apiml.response.compress"));
        if (!allowCompressionForService) {
            return false;
        }

        String routesToCompress = metadata.get("apiml.response.compressRoutes");
        if (routesToCompress == null) {
            return true;
        }

        String[] setOfRoutesToMatch = routesToCompress.split(",");
        AntPathMatcher matcher = new AntPathMatcher();
        for (String pattern: setOfRoutesToMatch) {
            if (!pattern.startsWith("/")) {
                pattern = "/" + pattern;
            }
            if (matcher.match(pattern, requestUri)) {
                return true;
            }
        }

        return false;
    }
}
