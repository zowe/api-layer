/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.hwsjersey.greeting;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;


public class GreetingControllerTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(GreetingController.class);
    }

    @Test
    public void givenUrlGreeting_whenSendRequest_thenReceiveGreeting() {
        Response response = target("api/v1/greeting").request().get();
        assertEquals(200, response.getStatus());
        Greeting greeting = response.readEntity(Greeting.class);
        assertEquals("Hello, World!", greeting.getContent());
    }

    @Test
    public void givenUrlGreetingWithName_whenSendRequest_thenReceiveGreetingwithName() {
        final String name = "Andrea";
        Response response = target("api/v1/greeting/" + name).request().get();
        assertEquals(200, response.getStatus());
        Greeting greeting = response.readEntity(Greeting.class);
        assertEquals("Hello, " + name + "!", greeting.getContent());
    }
}
