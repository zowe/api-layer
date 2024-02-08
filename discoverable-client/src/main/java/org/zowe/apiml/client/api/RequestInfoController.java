/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
@RestController
@RequestMapping("/api/v1/request")
@Tag(
    description = "/api/v1/request",
    name = "The request info API")
public class RequestInfoController {

    @GetMapping(
        value = "",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Returns all base information about request",
        description = "Data contains sign information, headers and content")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Information from request"),
        @ApiResponse(responseCode = "500", description = "Error in parsing of request ")
    })
    @ResponseBody
    public RequestInfo getRequestInfo(HttpServletRequest httpServletRequest) throws CertificateEncodingException, IOException {
        RequestInfo out = new RequestInfo();

        setCerts(httpServletRequest, out);
        setHeaders(httpServletRequest, out);
        setCookie(httpServletRequest, out);
        setContent(httpServletRequest, out);

        return out; // NOSONAR
    }

    private void setCerts(HttpServletRequest httpServletRequest, RequestInfo requestInfo) throws CertificateEncodingException {
        X509Certificate[] certs = (X509Certificate[]) httpServletRequest.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null) return;

        requestInfo.signed = certs.length > 0;
        requestInfo.certs = new Certificate[certs.length];
        for (int i = 0; i < certs.length; i++) {
            final X509Certificate cert = certs[i];
            final Certificate certDto = new Certificate(
                cert.getSerialNumber(),
                Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded()),
                Base64.getEncoder().encodeToString(cert.getEncoded())
            );
            requestInfo.certs[i] = certDto;
        }
    }

    private void setHeaders(HttpServletRequest httpServletRequest, RequestInfo requestInfo) {
        for (Enumeration<String> e = httpServletRequest.getHeaderNames(); e.hasMoreElements(); ) {
            final String name = e.nextElement();
            final String value = httpServletRequest.getHeader(name);
            requestInfo.headers.put(name, value);
        }
    }

    private void setCookie(HttpServletRequest httpServletRequest, RequestInfo requestInfo) {
        if (httpServletRequest.getCookies() == null) return;
        for (Cookie cookie : httpServletRequest.getCookies()) {
            requestInfo.cookies.put(cookie.getName(), cookie.getValue());
        }
    }

    private void setContent(HttpServletRequest httpServletRequest, RequestInfo requestInfo) throws IOException {
        requestInfo.content = httpServletRequest.getReader().lines().collect(Collectors.joining());
    }

    @Schema(description = "Request info detail")
    @Getter
    public class RequestInfo {

        @Schema(description = "Any certificate sign the request")
        private boolean signed;

        @Schema(description = "Certificates used to sign the request")
        private Certificate[] certs;

        @Schema(description = "Requests header in the original request")
        private Map<String, String> headers = new HashMap<>();

        @Schema(description = "Requests cookie in the original request")
        private Map<String, String> cookies = new HashMap<>();

        @Schema(description = "Text content in the original request")
        private String content;

    }

    @Schema(description = "Certificate info detail")
    @Value
    public class Certificate {

        private final BigInteger serialNo;
        private final String publicKeyEncodedBase64;
        private final String encodedBase64;

    }

}
