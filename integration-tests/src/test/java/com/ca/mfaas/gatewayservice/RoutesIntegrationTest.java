package com.ca.mfaas.gatewayservice;

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import com.ca.mfaas.utils.http.HttpRequestUtils;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

@Slf4j
public class RoutesIntegrationTest {

    private final static String APPLICATION_ROUTES_ENDPOINT = "/application/routes";
    private String token;
    private GatewayServiceConfiguration serviceConfiguration;
    private String scheme;
    private String host;
    private int port;
    private String basePath;
    @Before
    public void setUp() {
        serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();
        basePath = "/api/v1/gateway";

        RestAssured.port = port;
        RestAssured.basePath = basePath;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void shouldNotContainSuperfluousEndpoints() {
        URI uri = HttpRequestUtils.getUriFromGateway(APPLICATION_ROUTES_ENDPOINT);
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("/api/v1/api-doc/gateway/**", "apicatalog");
        given()
            .expect()
                .body("$",  Matchers.hasItem(expected))
            .when()
            .get(uri);
    }
        // Given
}
