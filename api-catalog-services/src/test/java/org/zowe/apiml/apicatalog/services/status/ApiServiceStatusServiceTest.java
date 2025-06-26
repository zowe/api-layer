/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.services.status;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.zowe.apiml.apicatalog.instance.InstanceInitializeService;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.services.status.event.model.ContainerStatusChangeEvent;
import org.zowe.apiml.apicatalog.services.status.event.model.STATUS_EVENT_TYPE;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiServiceStatusServiceTest {

    @Mock
    private InstanceInitializeService instanceInitializeService;

    @Test
    void givenContainers_whenGetContainersStateEvents_thenReturnEvents() {
        List<APIContainer> containers = new ArrayList<>(createContainers());
        when(instanceInitializeService.getAllContainers()).thenReturn(containers);
        doNothing().when(this.instanceInitializeService).calculateContainerServiceValues(any(APIContainer.class));

        List<ContainerStatusChangeEvent> expectedEvents = new ArrayList<>();
        containers.forEach(container -> {
            STATUS_EVENT_TYPE eventType;
            if (InstanceInfo.InstanceStatus.DOWN.name().equalsIgnoreCase(container.getStatus())) {
                eventType = STATUS_EVENT_TYPE.CANCEL;
            } else if (container.getCreatedTimestamp().equals(container.getLastUpdatedTimestamp())) {
                eventType = STATUS_EVENT_TYPE.CREATED_CONTAINER;
            } else {
                eventType = STATUS_EVENT_TYPE.RENEW;
            }
            expectedEvents.add(new ContainerStatusChangeEvent(
                container.getId(),
                container.getTitle(),
                container.getStatus(),
                container.getTotalServices(),
                container.getActiveServices(),
                container.getServices(),
                eventType)
            );
        });
    }

    private List<APIContainer> createContainers() {
        Set<APIService> services = new HashSet<>();

        APIService service = new APIService.Builder("service1")
            .title("service-1")
            .description("service-1")
            .secured(false)
            .baseUrl("base")
            .homePageUrl("home")
            .basePath("base")
            .sso(false)
            .apis(Collections.emptyMap())
            .build();
        services.add(service);

        service = new APIService.Builder("service2")
            .title("service-2")
            .description("service-2")
            .secured(true)
            .baseUrl("base")
            .homePageUrl("home")
            .basePath("base")
            .sso(false)
            .apis(Collections.emptyMap())
            .build();
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

}
