/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.eurekaservice.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppTest {
    @Test
    public void appTest() throws Exception {
        final String name = "name";
        final String description = "description";
        final String version = "version";

        App app = new App(name, description, version);
        assertEquals(app.getName(), name);
        assertEquals(app.getDescription(), description);
        assertEquals(app.getVersion(), version);
    }
}
