/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.hwsjersey.resource;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
@Ignore
public class GreetingControllerRoutingTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(GreetingController.class);
    }

    @Test
    public void whenSendRequest_sendDefaultGreeting() {
        Response response = target("v1/greeting").request().get();
        assertEquals(200, response.getStatus());
        Greeting greeting = response.readEntity(Greeting.class);
        assertEquals("Hello, World!", greeting.getContent());
    }

    @Test
    public void whenSendRequest_GivenAName_sendGreeting() {
        final String name = "Andrea";
        Response response = target("v1/greeting/" + name).request().get();
        assertEquals(200, response.getStatus());
        Greeting greeting = response.readEntity(Greeting.class);
        assertEquals("Hello, " + name + "!", greeting.getContent());
    }
}
