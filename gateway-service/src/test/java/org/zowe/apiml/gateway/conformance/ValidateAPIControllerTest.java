/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.CoreMatchers.containsString;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;

import java.io.IOException;

@AcceptanceTest
public class ValidateAPIControllerTest extends AcceptanceTestWithTwoServices {
    
    private String validatePath;
    private MessageService messageService;
    private ValidateAPIController validateAPIController;
    
    @BeforeEach
    void setup() throws IOException {
        validatePath = "/validate";
    }

    @Nested
    class GivenControllerServiceID {

        @BeforeEach
        void setup() throws IOException {
            messageService = new YamlMessageService("/gateway-log-messages.yml");
            validateAPIController = new ValidateAPIController(messageService);
            standaloneSetup(validateAPIController);
        }

        @Test
        void whenServiceId_validate() throws Exception {
            given()  
                .param("serviceID","validserviceid")
            .when()          
                .post(basePath + validatePath)
            .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
        }

        @Test
        void whenServiceId_InvalidateUpper() throws Exception {
           
            given()
                .param("serviceID", "Invalidserviceidcontainupperletter")
            .when()
                .post(basePath + validatePath)
            .then()
                .assertThat()
                .body("messageNumber", equalTo("ZWEAG717E"),
                    "messageContent", containsString("The serviceid contains symbols or upper case letters"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        void whenServiceId_InvalidateSymbol() throws Exception {
            given()
                .param("serviceID", "invalid@serviceid_containsymbols")
            .when()
                .post(basePath + validatePath)
            .then()
                .assertThat()
                .body("messageNumber", equalTo("ZWEAG717E"),
                    "messageContent", containsString("The serviceid contains symbols or upper case letters"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        
        @Test
        void whenServiceId_InvalidateLength() throws Exception {
            given()
                .param("serviceID", "qwertyuiopasdfghjklzxcvbnmqwertyuiopasdfghjklwsezxcvbnmqwertyuiop")
            .when()
                .post(basePath + validatePath)
            .then()
                .assertThat()
                .body("messageNumber", equalTo("ZWEAG717E"),
                    "messageContent", containsString("The serviceid is longer than 64 characters"))
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        }
        
    }
}