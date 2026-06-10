/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CustomHstsHeadersWriterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private CustomHstsHeadersWriter writer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        writer = new CustomHstsHeadersWriter();
    }

    @Test
    void shouldWriteHstsHeader() {

        writer.writeHeaders(request, response);

        verify(response, times(1)).setHeader(
            "Strict-Transport-Security",
            "max-age=31536000; includeSubDomains"
        );
    }

    @Test
    void shouldWriteHeaderWithCorrectDurationAndSubdomains() {

        final long expectedDuration = Duration.ofDays(365L).getSeconds();
        final String expectedHeaderValue = "max-age=" + expectedDuration + "; includeSubDomains";

        writer.writeHeaders(request, response);

        verify(response).setHeader("Strict-Transport-Security", expectedHeaderValue);
    }

}
