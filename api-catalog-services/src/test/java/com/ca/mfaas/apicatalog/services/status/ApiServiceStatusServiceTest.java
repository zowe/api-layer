/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status;

import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.model.APIService;
import com.ca.mfaas.apicatalog.services.cached.CachedApiDocService;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiServiceStatusServiceTest {

    @Mock
    private CachedApiDocService cachedApiDocService;

    @InjectMocks
    private APIServiceStatusService apiServiceStatusService;

    @Test
    public void getCachedApiDocInfoForService() throws IOException {
        String apiDoc = "this is the api doc";
        when(cachedApiDocService.getApiDocForService(anyString(), anyString())).thenReturn(apiDoc);
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(apiDoc, HttpStatus.OK);
        ResponseEntity<String> actualResponse = apiServiceStatusService.getServiceCachedApiDocInfo("aaa", "v1");
        assertEquals(expectedResponse.getStatusCode(), actualResponse.getStatusCode());
        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    }

    private List<APIContainer> createContainers() {
        Set<APIService> services = new HashSet<>();

        APIService service = new APIService("service1", "service-1", "service-1", false, "home");
        services.add(service);

        service = new APIService("service2", "service-2", "service-2", true, "home");
        services.add(service);

        APIContainer container = new APIContainer("api-one", "API One", "This is API One", services);
        container.setTotalServices(2);
        container.setActiveServices(2);
        container.setStatus(InstanceInfo.InstanceStatus.UP.name());
        APIContainer container1 = new APIContainer("api-two", "API Two", "This is API Two", services);
        container1.setTotalServices(2);
        container1.setActiveServices(2);
        container.setStatus(InstanceInfo.InstanceStatus.DOWN.name());
        return Arrays.asList(container, container1);
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status,
            HashMap<String, String> metadata, String ipAddress, int port) {
        return new InstanceInfo(serviceId + ":" + port, serviceId.toUpperCase(), null, ipAddress, null,
                new InstanceInfo.PortWrapper(true, port), null, null, null, null, null, null, null, 0, null, "hostname",
                status, null, null, null, null, metadata, null, null, null, null);
    }
}
