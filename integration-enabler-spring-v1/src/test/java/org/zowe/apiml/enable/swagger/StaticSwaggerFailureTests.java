/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable.swagger;

import org.zowe.apiml.SpringBootTestClassForIntegrationEnablement;
import org.zowe.apiml.enable.api.ApiDocController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileNotFoundException;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "eureka.client.enabled=false",
        "mfaas.discovery.info.swaggerLocation=missing_swagger.json"
    },
    classes = {SpringBootTestClassForIntegrationEnablement.class}
)
public class StaticSwaggerFailureTests {

    @Autowired
    private ApiDocController apiDocController;

    @Test(expected = FileNotFoundException.class)
    public void givenASwaggerLocationThatDoesntExistThenFail() throws IOException {
        apiDocController.getApiDoc(null);
    }
}
