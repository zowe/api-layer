/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.hwsjersey.health;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;
import org.zowe.apiml.eurekaservice.model.Health;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import java.util.HashMap;

import static org.junit.Assert.*;

public class HealthControllerTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(HealthController.class);
    }

    @Test
    public void givenUrlApplicationInfo_whenSendRequest_thenReceiveInfoAttributesAsEmpty() {
        Response response = target("application/info").request().get();
        assertEquals(200, response.getStatus());
        HashMap infoAttributes = response.readEntity(HashMap.class);
        assertTrue("InfoAttributes map is not empty", infoAttributes.isEmpty());
    }

    @Test
    public void givenUrlApplicationHealth_whenSendRequest_thenReceiveHealthAsUp() {
        Response response = target("application/health").request().get();
        assertEquals(200, response.getStatus());

        Health actualHealthStatus = response.readEntity(Health.class);
        Health expectedHealthStatus = new Health("UP");

        assertEquals("Expected health status is not UP", expectedHealthStatus.getStatus(), actualHealthStatus.getStatus());
    }

}
