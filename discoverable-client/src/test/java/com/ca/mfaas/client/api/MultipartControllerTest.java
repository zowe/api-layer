/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.client.api;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ModelMap;

import static org.junit.Assert.assertEquals;


public class MultipartControllerTest {
    private MultipartController multipartController;

    @Before
    public void setup() {
        multipartController = new MultipartController();
    }

    @Test
    public void shouldSubmitAndReturnString() {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes());
        ModelMap model = new ModelMap();

        assertEquals("fileUploadView", multipartController.submit(file, model));
    }
}
