/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.client.multipart;

import com.ca.mfaas.client.controller.controllers.api.MultipartController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ModelMap;


public class MultipartControllerTest {
    private MultipartController multipartController;

    @Before
    public void setup() {
        multipartController = new MultipartController();
    }

    @Test
    public void shouldSubmitAndReturnString() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "hello.txt", MediaType.TEXT_PLAIN_VALUE, "Hello, World!".getBytes());
        ModelMap model = new ModelMap();
        Assert.assertEquals("fileUploadView", multipartController.submit(file, model));

    }
}
