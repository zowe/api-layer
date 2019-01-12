package com.ca.mfaas.gateway.config;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MultipartConfigTest {
    @Test
    public void shouldDoPutRequestAndReturnTrue() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/");
        request.setContentType("multipart/");
        MultipartConfig multipartConfig = new MultipartConfig();
        assertTrue(multipartConfig.multipartResolver().isMultipart(request));
    }

    @Test
    public void shouldDoGetRequestAndReturnFalse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/");
        request.setContentType("multipart/");
        MultipartConfig multipartConfig = new MultipartConfig();
        assertFalse(multipartConfig.multipartResolver().isMultipart(request));
    }
}
