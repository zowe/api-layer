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
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.gzip.GZipResponseUtils;
import org.zowe.apiml.gzip.GZipResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
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


    private boolean requiresCompression(HttpServletRequest request) {
        String[] uriParts = request.getRequestURI().split("/");
        List<ServiceInstance> instances;
        if ("api".equals(uriParts[1]) || "ui".equals(uriParts[1])) {
            instances = discoveryClient.getInstances(uriParts[3]);
        } else {
            instances = discoveryClient.getInstances(uriParts[1]);
        }
        if (instances == null || instances.isEmpty()) {
            return false;
        }
        return "true".equals(instances.get(0).getMetadata().get("apiml.response.compress"));
    }


}
