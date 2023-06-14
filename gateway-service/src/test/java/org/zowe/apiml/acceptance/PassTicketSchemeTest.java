/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import io.restassured.http.Cookie;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;
import org.zowe.apiml.acceptance.config.PassTicketFailureConfig;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

/**
 * This test verifies that a REST message is returned in case of failure during generation of PassTicket.
 */
@AcceptanceTest
@Import(PassTicketFailureConfig.class)
public class PassTicketSchemeTest extends AcceptanceTestWithTwoServices {

    @Nested
    class GivenValidJwtToken {
        Cookie validJwtToken;

        @BeforeEach
        void setUp() {
            validJwtToken = securityRequests.validJwtToken();
        }

        @Nested
        class WhenPassTicketRequestedByService {

            @BeforeEach
            void prepareService() {
                applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
            }

            @Test
            void thenPassTicketErrorMessageIsReturned() {

                given()
                    .log().all()
                    .cookie(validJwtToken)
                    .when()
                    .get(basePath + serviceWithDefaultConfiguration.getPath())
                    .then()
                    .log().all()
                    .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR)).body("messages[0].messageKey",is("org.zowe.apiml.security.ticket.generateFailed"));
            }
        }
    }
}

