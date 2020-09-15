/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.startup;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.EnvironmentCheck;
import org.zowe.apiml.util.config.ConfigReader;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;

@EnvironmentCheck
class CheckEnvironment {

    @Test
    void checkZosmfIsUpAndApimtstIsWorking() {
        String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
        String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();

        String ZOSMF_HOST = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getHost();
        int ZOSMF_PORT = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getPort();
        String ZOSMF_AUTH_ENDPOINT = "/zosmf/services/authenticate";
        String ZOSMF_PROTECTED_ENDPOINT = "/zosmf/restfiles/ds?dslevel=sys1.p*";
        String ZOSMF_SCHEME = ConfigReader.environmentConfiguration().getZosmfServiceConfiguration().getScheme();

        // login with Basic and get JWT
        String basicJWT =
            given().auth().basic(USERNAME, PASSWORD)
                .header("X-CSRF-ZOSMF-HEADER", "")
                .when()
                //.post("https://usilca32.lvn.broadcom.net:1443/zosmf/services/authenticate")
                .post(String.format("%s://%s:%d%s", ZOSMF_SCHEME, ZOSMF_HOST, ZOSMF_PORT, ZOSMF_AUTH_ENDPOINT))
                .then().statusCode(is(SC_OK))
                .extract().cookie("jwtToken");

        // call zOSMF with it
        given()
            .cookie("jwtToken", basicJWT)
            .header("X-CSRF-ZOSMF-HEADER", "")
            .when()
            //.get("https://usilca32.lvn.broadcom.net:1443/zosmf/restfiles/ds?dslevel=sys1.p*")
            .get(String.format("%s://%s:%d%s", ZOSMF_SCHEME, ZOSMF_HOST, ZOSMF_PORT, ZOSMF_PROTECTED_ENDPOINT))
            .then().statusCode(SC_OK);
    }
}
