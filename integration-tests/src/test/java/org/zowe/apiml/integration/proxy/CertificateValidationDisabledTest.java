package org.zowe.apiml.integration.proxy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.zowe.apiml.util.http.HttpRequestUtils;
import java.net.URI;
import org.zowe.apiml.util.requests.Endpoints;

import io.restassured.RestAssured;

import static io.restassured.RestAssured.given;

@Tag("UnknownHostnamesTest")
public class CertificateValidationDisabledTest {
    @Test
    void givenRequestToServiceWithInvalidHostname_thenRequestIsSuccessful() {
        RestAssured.useRelaxedHTTPSValidation();
        URI uri = HttpRequestUtils.getUriFromGateway(Endpoints.DISCOVERABLE_GREET);
        given()
            .log().ifValidationFails()
            .get(uri)
            .then()
            .log().ifValidationFails()
            .statusCode(200);
    }
}
