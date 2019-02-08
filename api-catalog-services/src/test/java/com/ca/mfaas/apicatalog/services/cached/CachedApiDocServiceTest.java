/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.cached;

import com.ca.mfaas.apicatalog.services.status.APIDocRetrievalService;
import com.ca.mfaas.apicatalog.swagger.TransformApiDocService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.when;

public class CachedApiDocServiceTest {

    @Test
    public void testRetrievalOfApiDocWhenApiIsAvailable() {
        String expectedApiDoc = "This is some api doc";
        ResponseEntity<String> response = new ResponseEntity<>(expectedApiDoc, HttpStatus.OK);
        APIDocRetrievalService apiDocRetrievalService = Mockito.mock(APIDocRetrievalService.class);
        TransformApiDocService transformApiDocService = Mockito.mock(TransformApiDocService.class);
        when(apiDocRetrievalService.retrieveApiDoc("service2", "2.0.0")).thenReturn(response);

        CachedApiDocService cachedApiDocService = new CachedApiDocService(apiDocRetrievalService, transformApiDocService);
        String apiDoc = cachedApiDocService.getApiDocForService("service2", "2.0.0");
        Assert.assertNotNull(apiDoc);
        Assert.assertEquals(expectedApiDoc, apiDoc);
    }

    @Test
    public void testUpdateOfApiDocForService() {
        String expectedApiDoc = "This is some api doc";
        ResponseEntity<String> response = new ResponseEntity<>(expectedApiDoc, HttpStatus.OK);
        APIDocRetrievalService apiDocRetrievalService = Mockito.mock(APIDocRetrievalService.class);
        TransformApiDocService transformApiDocService = Mockito.mock(TransformApiDocService.class);
        when(apiDocRetrievalService.retrieveApiDoc("service1", "1.0.0")).thenReturn(response);

        CachedApiDocService cachedApiDocService = new CachedApiDocService(apiDocRetrievalService, transformApiDocService);

        String apiDoc = cachedApiDocService.getApiDocForService("service1", "1.0.0");
        Assert.assertNotNull(apiDoc);
        Assert.assertEquals(expectedApiDoc, apiDoc);

        String updatedApiDoc = "This is some updated API Doc";
        cachedApiDocService.updateApiDocForService("service1", "1.0.0", updatedApiDoc);

        response = new ResponseEntity<>(updatedApiDoc, HttpStatus.OK);
        when(apiDocRetrievalService.retrieveApiDoc("service1", "1.0.0")).thenReturn(response);

        apiDoc = cachedApiDocService.getApiDocForService("service1", "1.0.0");
        Assert.assertNotNull(apiDoc);
        Assert.assertEquals(updatedApiDoc, apiDoc);

    }
}
