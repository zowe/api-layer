/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.services.cached;

import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.services.status.APIDocRetrievalService;
import org.zowe.apiml.apicatalog.swagger.TransformApiDocService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CachedApiDocServiceTest {
    private CachedApiDocService cachedApiDocService;

    @Mock
    APIDocRetrievalService apiDocRetrievalService;

    @Mock
    TransformApiDocService transformApiDocService;

    @Before
    public void setUp() {
        cachedApiDocService = new CachedApiDocService(apiDocRetrievalService, transformApiDocService);
        cachedApiDocService.resetCache();
    }

    @Test
    public void testRetrievalOfApiDocWhenApiIsAvailable() {
        String serviceId = "Service";
        String version = "v1";
        String expectedApiDoc = "This is some api doc";

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, expectedApiDoc, null);

        when(apiDocRetrievalService.retrieveApiDoc(serviceId, version))
            .thenReturn(apiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, apiDocInfo))
            .thenReturn(expectedApiDoc);

        String apiDoc = cachedApiDocService.getApiDocForService(serviceId, version);

        Assert.assertNotNull(apiDoc);
        Assert.assertEquals(expectedApiDoc, apiDoc);
    }

    @Test
    public void testUpdateOfApiDocForService() {
        String serviceId = "Service";
        String version = "v1";
        String expectedApiDoc = "This is some api doc";
        String updatedApiDoc = "This is some updated API Doc";


        ApiDocInfo apiDocInfo = new ApiDocInfo(null, expectedApiDoc, null);

        when(apiDocRetrievalService.retrieveApiDoc(serviceId, version))
            .thenReturn(apiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, apiDocInfo))
            .thenReturn(expectedApiDoc);

        String apiDoc = cachedApiDocService.getApiDocForService(serviceId, version);

        Assert.assertNotNull(apiDoc);
        Assert.assertEquals(expectedApiDoc, apiDoc);

        cachedApiDocService.updateApiDocForService(serviceId, version, updatedApiDoc);

        apiDocInfo = new ApiDocInfo(null, updatedApiDoc, null);

        when(apiDocRetrievalService.retrieveApiDoc(serviceId, version))
            .thenReturn(apiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, apiDocInfo))
            .thenReturn(updatedApiDoc);

        apiDoc = cachedApiDocService.getApiDocForService(serviceId, version);

        Assert.assertNotNull(apiDoc);
        Assert.assertEquals(updatedApiDoc, apiDoc);
    }

    @Test
    public void shouldReturnNullIfNotValidResponse() {
        String serviceId = "Service";
        String version = "v1";
        String expectedApiDoc = "This is some api doc";

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, null, null);

        when(apiDocRetrievalService.retrieveApiDoc(serviceId, version))
            .thenReturn(apiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, apiDocInfo))
            .thenReturn(expectedApiDoc);

        String apiDoc = cachedApiDocService.getApiDocForService(serviceId, version);

        Assert.assertNull(apiDoc);
    }
}
