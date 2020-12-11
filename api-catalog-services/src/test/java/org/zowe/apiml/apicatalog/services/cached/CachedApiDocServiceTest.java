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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.services.status.APIDocRetrievalService;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.services.status.model.ApiVersionNotFoundException;
import org.zowe.apiml.apicatalog.swagger.TransformApiDocService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CachedApiDocServiceTest {
    private CachedApiDocService cachedApiDocService;

    @Mock
    APIDocRetrievalService apiDocRetrievalService;

    @Mock
    TransformApiDocService transformApiDocService;

    @BeforeEach
    private void setUp() {
        cachedApiDocService = new CachedApiDocService(apiDocRetrievalService, transformApiDocService);
        cachedApiDocService.resetCache();
    }

    @Test
    void givenValidApiDoc_whenRetrieving_thenReturnIt() {
        String serviceId = "Service";
        String version = "v1";
        String expectedApiDoc = "This is some api doc";

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, expectedApiDoc, null);

        when(apiDocRetrievalService.retrieveApiDoc(serviceId, version))
            .thenReturn(apiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, apiDocInfo))
            .thenReturn(expectedApiDoc);

        String apiDoc = cachedApiDocService.getApiDocForService(serviceId, version);

        assertNotNull(apiDoc);
        assertEquals(expectedApiDoc, apiDoc);
    }

    @Test
    void givenValidApiDoc_whenUpdating_thenRetrieve() {
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

        assertNotNull(apiDoc);
        assertEquals(expectedApiDoc, apiDoc);

        cachedApiDocService.updateApiDocForService(serviceId, version, updatedApiDoc);

        apiDocInfo = new ApiDocInfo(null, updatedApiDoc, null);

        when(apiDocRetrievalService.retrieveApiDoc(serviceId, version))
            .thenReturn(apiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, apiDocInfo))
            .thenReturn(updatedApiDoc);

        apiDoc = cachedApiDocService.getApiDocForService(serviceId, version);

        assertNotNull(apiDoc);
        assertEquals(updatedApiDoc, apiDoc);
    }

    @Test
    void givenInvalidApiDoc_whenRetrieving_thenThrowException() {
        String serviceId = "Service";
        String version = "v1";

        Exception exception = assertThrows(ApiDocNotFoundException.class,
            () -> cachedApiDocService.getApiDocForService(serviceId, version),
            "Expected exception is not ApiDocNotFoundException");
        assertEquals("No API Documentation was retrieved for the service Service.", exception.getMessage());
    }

    @Test
    void givenValidApiVersions_whenRetrieving_thenReturnIt() {
        String serviceId = "service";
        List<String> expectedVersions = Arrays.asList("1.0.0", "2.0.0");

        when(apiDocRetrievalService.retrieveApiVersions(serviceId)).thenReturn(expectedVersions);

        List<String> versions = cachedApiDocService.getApiVersionsForService(serviceId);
        assertEquals(expectedVersions, versions);
    }

    @Test
    void givenValidApiVersions_whenUpdating_thenRetrieve() {
        String serviceId = "service";
        List<String> initialVersions = Arrays.asList("1.0.0", "2.0.0");
        List<String> updatedVersions = Arrays.asList("1.0.0", "2.0.0", "3.0.0");

        when(apiDocRetrievalService.retrieveApiVersions(serviceId)).thenReturn(initialVersions);

        List<String> versions = cachedApiDocService.getApiVersionsForService(serviceId);
        assertEquals(initialVersions, versions);

        cachedApiDocService.updateApiVersionsForService(serviceId, updatedVersions);

        when(apiDocRetrievalService.retrieveApiVersions(serviceId)).thenReturn(updatedVersions);

        versions = cachedApiDocService.getApiVersionsForService(serviceId);
        assertEquals(updatedVersions, versions);
    }

    @Test
    void givenInvalidApiVersion_whenRetrieving_thenThrowException() {
        String serviceId = "service";

        when(apiDocRetrievalService.retrieveApiVersions(serviceId)).thenReturn(Collections.emptyList());

        Exception exception = assertThrows(ApiVersionNotFoundException.class,
            () -> cachedApiDocService.getApiVersionsForService(serviceId), "Exception is not ApiVersionsNotFoundException");
        assertEquals("No API versions were retrieved for the service service.", exception.getMessage());
    }

    @Test
    void givenValidApiDocs_whenRetrievingDefault_thenReturnLatestApi() {
        String serviceId = "service";
        String expectedApiDoc = "This is some api doc";
        ApiDocInfo apiDocInfo = new ApiDocInfo(null, expectedApiDoc, null);

        when(apiDocRetrievalService.retrieveDefaultApiDoc(serviceId)).thenReturn(apiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, apiDocInfo)).thenReturn(expectedApiDoc);

        String apiDoc = cachedApiDocService.getDefaultApiDocForService(serviceId);
        assertEquals(expectedApiDoc, apiDoc);
    }

    @Test
    void givenValidApiDocs_whenUpdatingDefault_thenRetrieveDefault() {
        String serviceId = "service";
        String initialApiDoc = "This is some api doc";
        String updatedApiDoc = "This is some updated api doc";
        ApiDocInfo initialApiDocInfo = new ApiDocInfo(null, initialApiDoc, null);
        ApiDocInfo updatedApiDocInfo = new ApiDocInfo(null, updatedApiDoc, null);

        when(apiDocRetrievalService.retrieveDefaultApiDoc(serviceId)).thenReturn(initialApiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, initialApiDocInfo)).thenReturn(initialApiDoc);

        String apiDoc = cachedApiDocService.getDefaultApiDocForService(serviceId);
        assertEquals(initialApiDoc, apiDoc);

        cachedApiDocService.updateDefaultApiDocForService(serviceId, updatedApiDoc);

        when(apiDocRetrievalService.retrieveDefaultApiDoc(serviceId)).thenReturn(updatedApiDocInfo);
        when(transformApiDocService.transformApiDoc(serviceId, updatedApiDocInfo)).thenReturn(updatedApiDoc);

        apiDoc = cachedApiDocService.getDefaultApiDocForService(serviceId);
        assertEquals(updatedApiDoc, apiDoc);
    }

    @Test
    void givenInvalidApiDoc_whenRetrievingDefault_thenThrowException() {
        String serviceId = "service";

        Exception exception = assertThrows(ApiDocNotFoundException.class,
            () -> cachedApiDocService.getDefaultApiDocForService(serviceId),
            "Expected exception is not ApiDocNotFoundException");
        assertEquals("No API Documentation was retrieved for the service service.", exception.getMessage());
    }

    @Test
    void givenDefaultApiVersion_whenRetrieveDefaultVersion_thenReturnIt() {
        String serviceId = "service";
        String expected = "v1";
        when(apiDocRetrievalService.retrieveDefaultApiVersion(serviceId)).thenReturn(expected);

        String actual = cachedApiDocService.getDefaultApiVersionForService(serviceId);
        assertEquals(expected, actual);
    }

    @Test
    void givenDefaultApiVersion_whenUpdateDefaultVersion_thenRetrieveDefault() {
        String serviceId = "service";
        String initialVersion = "v1";
        String updatedVersion = "v2";

        when(apiDocRetrievalService.retrieveDefaultApiVersion(serviceId)).thenReturn(initialVersion);
        String version = cachedApiDocService.getDefaultApiVersionForService(serviceId);
        assertEquals(initialVersion, version);

        cachedApiDocService.updateDefaultApiVersionForService(serviceId, updatedVersion);

        when(apiDocRetrievalService.retrieveDefaultApiVersion(serviceId)).thenReturn(null);
        version = cachedApiDocService.getDefaultApiVersionForService(serviceId);
        assertEquals(updatedVersion, version);
    }

    @Test
    void givenErrorRetrievingDefaultApiVersion_whenGetDefaultVersion_thenThrowException() {
        String serviceId = "service";

        when(apiDocRetrievalService.retrieveDefaultApiVersion(serviceId)).thenThrow(new RuntimeException("error"));

        Exception exception = assertThrows(ApiVersionNotFoundException.class,
            () -> cachedApiDocService.getDefaultApiVersionForService(serviceId));
        assertEquals("Error trying to find default API version", exception.getMessage());
    }
}
