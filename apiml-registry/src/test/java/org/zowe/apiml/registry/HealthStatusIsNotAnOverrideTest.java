/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.model.DataCenterInfo;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.PortInfo;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A status a service reports about itself must not become an operator override.
 * <p>
 * {@code eureka.client.healthcheck.enabled} makes a service advertise {@code UP} when its health indicators pass
 * and {@code DOWN} when they do not. That change travels as a re-registration, so the stored instance's own
 * {@code status} moves while {@code overriddenStatus} stays {@code UNKNOWN}.
 * <p>
 * Routing a health change through the status-override path instead - which is what the client used to do - made
 * it permanent. {@code ServiceInstance.effectiveStatus()} returns {@code overriddenStatus} whenever it is set,
 * and an override is only cleared explicitly, so a service that went unhealthy once during startup and then
 * recovered was still served as {@code DOWN} for the life of the process. The Gateway hitting that path broke
 * {@code /application/**} authentication on the Discovery Service - ZWEAS120E, "Invalid username or password" -
 * because the login provider could no longer reach a Gateway the registry reported as down.
 */
class HealthStatusIsNotAnOverrideTest {

    private static final String APP = "GATEWAY";
    private static final String INSTANCE = "localhost:gateway:10010";

    private static ServiceInstance gateway(InstanceStatus status) {
        return ServiceInstance.builder()
            .instanceId(INSTANCE)
            .appName(APP)
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(new PortInfo(10010, false))
            .securePort(new PortInfo(10010, true))
            .homePageUrl("https://localhost:10010/")
            .status(status)
            .dataCenterInfo(DataCenterInfo.MY_OWN)
            .build();
    }

    private static ServiceInstance stored(InMemoryServiceRegistry registry) {
        return registry.instance(APP, INSTANCE).orElseThrow();
    }

    private static void reRegister(InMemoryServiceRegistry registry, InstanceStatus status) {
        // What RegistryClient.updateStatus does: convey the new status as a registration.
        registry.register(gateway(status), RegistrationKind.DYNAMIC);
    }

    @Test
    void anUnhealthyServiceIsNotRecordedAsAnOperatorOverride() {
        InMemoryServiceRegistry registry =
            new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), System::currentTimeMillis);
        registry.openForTraffic(1);

        registry.register(gateway(InstanceStatus.UP), RegistrationKind.DYNAMIC);
        assertEquals(InstanceStatus.UP, stored(registry).status());
        assertEquals(InstanceStatus.UNKNOWN, stored(registry).overriddenStatus());

        reRegister(registry, InstanceStatus.DOWN);

        assertEquals(InstanceStatus.DOWN, stored(registry).status(),
            "the reported status must move to DOWN");
        assertEquals(InstanceStatus.UNKNOWN, stored(registry).overriddenStatus(),
            "a health change must not create an override");
        assertEquals(InstanceStatus.DOWN, stored(registry).effectiveStatus());
    }

    @Test
    void aServiceThatRecoversBecomesRoutableAgain() {
        InMemoryServiceRegistry registry =
            new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), System::currentTimeMillis);
        registry.openForTraffic(1);

        registry.register(gateway(InstanceStatus.UP), RegistrationKind.DYNAMIC);
        reRegister(registry, InstanceStatus.DOWN);
        reRegister(registry, InstanceStatus.UP);

        assertEquals(InstanceStatus.UP, stored(registry).status(),
            "the instance must be able to come back up");
        assertEquals(InstanceStatus.UNKNOWN, stored(registry).overriddenStatus(),
            "no override can be left behind to pin it down");
        assertEquals(InstanceStatus.UP, stored(registry).effectiveStatus());
    }

    @Test
    void aTransientDownDoesNotOutliveTheHealthCheckThatCausedIt() {
        InMemoryServiceRegistry registry =
            new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), System::currentTimeMillis);
        registry.openForTraffic(1);

        registry.register(gateway(InstanceStatus.UP), RegistrationKind.DYNAMIC);
        // Startup churn: several unhealthy reports before the service settles.
        for (int i = 0; i < 5; i++) {
            reRegister(registry, InstanceStatus.DOWN);
        }
        reRegister(registry, InstanceStatus.UP);

        assertEquals(InstanceStatus.UP, stored(registry).effectiveStatus(),
            "however many times it went down, recovery must be honoured");
    }

    @Test
    void anOperatorOverrideStillBeatsWhatTheServiceReports() {
        InMemoryServiceRegistry registry =
            new InMemoryServiceRegistry(RegistrySettings.defaults(), List.of(), System::currentTimeMillis);
        registry.openForTraffic(1);

        registry.register(gateway(InstanceStatus.UP), RegistrationKind.DYNAMIC);
        // The explicit override endpoint: an operator taking the instance out of service.
        assertTrue(registry.overrideStatus(APP, INSTANCE, InstanceStatus.OUT_OF_SERVICE, false));

        reRegister(registry, InstanceStatus.UP);

        assertEquals(InstanceStatus.OUT_OF_SERVICE, stored(registry).effectiveStatus(),
            "an operator's decision must survive the service reporting itself healthy");
        assertEquals(InstanceStatus.OUT_OF_SERVICE, stored(registry).overriddenStatus());
    }

}
