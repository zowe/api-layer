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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.services.status.APIDocRetrievalService;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.services.status.model.ApiVersionsNotFoundException;
import org.zowe.apiml.apicatalog.swagger.TransformApiDocService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    public void givenValidApiDoc_whenRetrieving_thenReturnIt() {
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
    public void givenValidApiDoc_whenUpdating_thenRetrieve() {
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
    public void givenInvalidApiDoc_whenRetrieving_thenThrowException() {
        String serviceId = "Service";
        String version = "v1";

        Exception exception = assertThrows(ApiDocNotFoundException.class,
            () -> cachedApiDocService.getApiDocForService(serviceId, version),
            "Expected exception is not ApiDocNotFoundException");
        assertEquals("No API Documentation was retrieved for the service Service.", exception.getMessage());
    }

    @Test
    public void givenValidApiVersions_whenRetrieving_thenReturnIt() {
        String serviceId = "service";
        List<String> expectedVersions = Arrays.asList("1.0.0", "2.0.0");

        when(apiDocRetrievalService.retrieveApiVersions(serviceId)).thenReturn(expectedVersions);

        List<String> versions = cachedApiDocService.getApiVersionsForService(serviceId);
        Assert.assertEquals(expectedVersions, versions);
    }

    @Test
    public void givenValidApiVersions_whenUpdating_thenRetrieve() {
        String serviceId = "service";
        List<String> initialVersions = Arrays.asList("1.0.0", "2.0.0");
        List<String> updatedVersions = Arrays.asList("1.0.0", "2.0.0", "3.0.0");

        when(apiDocRetrievalService.retrieveApiVersions(serviceId)).thenReturn(initialVersions);

        List<String> versions = cachedApiDocService.getApiVersionsForService(serviceId);
        Assert.assertEquals(initialVersions, versions);

        cachedApiDocService.updateApiVersionsForService(serviceId, updatedVersions);

        when(apiDocRetrievalService.retrieveApiVersions(serviceId)).thenReturn(updatedVersions);

        versions = cachedApiDocService.getApiVersionsForService(serviceId);
        Assert.assertEquals(updatedVersions, versions);
    }

    @Test
    public void givenInvalidApiVersion_whenRetrieving_thenThrowException() {
        String serviceId = "service";

        when(apiDocRetrievalService.retrieveApiVersions(serviceId)).thenReturn(Collections.emptyList());

        Exception exception = assertThrows(ApiVersionsNotFoundException.class,
            () -> cachedApiDocService.getApiVersionsForService(serviceId), "Exception is not ApiVersionsNotFoundException");
        assertEquals("No API versions were retrieved for the service service.", exception.getMessage());
    }

    @Test
    public void givenValidApiDocs_whenRetrievingDefault_thenReturnLatestApi() {
        String serviceId = "service";
        String expectedApiDoc = "This is some api doc";
        ApiDocInfo apiDocInfo = new ApiDocInfo(null, expectedApiDoc, null);

        when(apiDocRetrievalService.retrieveDefaultApiDoc(serviceId)).thenReturn(apiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, apiDocInfo)).thenReturn(expectedApiDoc);

        String apiDoc = cachedApiDocService.getDefaultApiDocForService(serviceId);
        Assert.assertEquals(expectedApiDoc, apiDoc);
    }

    @Test
    public void givenValidApiDocs_whenUpdatingDefault_thenRetrieveDefault() {
        String serviceId = "service";
        String initialApiDoc = "This is some api doc";
        String updatedApiDoc = "This is some updated api doc";
        ApiDocInfo initialApiDocInfo = new ApiDocInfo(null, initialApiDoc, null);
        ApiDocInfo updatedApiDocInfo = new ApiDocInfo(null, updatedApiDoc, null);

        when(apiDocRetrievalService.retrieveDefaultApiDoc(serviceId)).thenReturn(initialApiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, initialApiDocInfo)).thenReturn(initialApiDoc);

        String apiDoc = cachedApiDocService.getDefaultApiDocForService(serviceId);
        Assert.assertEquals(initialApiDoc, apiDoc);

        cachedApiDocService.updateLatestApiDocForService(serviceId, updatedApiDoc);

        when(apiDocRetrievalService.retrieveDefaultApiDoc(serviceId)).thenReturn(updatedApiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, updatedApiDocInfo)).thenReturn(updatedApiDoc);

        apiDoc = cachedApiDocService.getDefaultApiDocForService(serviceId);
        Assert.assertEquals(updatedApiDoc, apiDoc);
    }

    @Test
    public void givenInvalidApiDoc_whenRetrievingDefault_thenThrowException() {
        String serviceId = "service";

        Exception exception = assertThrows(ApiDocNotFoundException.class,
            () -> cachedApiDocService.getDefaultApiDocForService(serviceId),
            "Expected exception is not ApiDocNotFoundException");
        assertEquals("No API Documentation was retrieved for the service service.", exception.getMessage());
    }
}
