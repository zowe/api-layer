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

import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class GreetingControllerTest {

        @Test
        public void whenSendRequest_GivenAName_sendGreeting() throws Exception {
            GreetingController greetingController = new GreetingController();
            Response response = greetingController.defaultGreeting();
            assertEquals(200, response.getStatus());
            Greeting greeting = (Greeting) response.getEntity();
            assertEquals("Hello, World!", greeting.getContent());
        }

        @Test
        public void whenSendRequest_sendDefaultGreeting() throws Exception {
            final String name = "Andrea";
            GreetingController greetingController = new GreetingController();
            Response response = greetingController.greeting(name);
            assertEquals(200, response.getStatus());
            Greeting greeting = (Greeting) response.getEntity();
            assertEquals("Hello, " + name + "!", greeting.getContent());
        }
}
