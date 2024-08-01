/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.token;

import org.apache.http.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.zowe.apiml.acceptance.config.ApimlRoutingConfig;
import org.zowe.apiml.acceptance.config.DiscoveryClientTestConfig;
import org.zowe.apiml.acceptance.config.GatewayOverrideConfig;
import org.zowe.apiml.acceptance.config.GatewayTestApplication;

import java.time.Instant;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.constants.ApimlConstants.BEARER_AUTHENTICATION_PREFIX;

@SpringBootTest(classes = GatewayTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
       "apiml.security.oidc.validationType=endpoint",
        "apiml.security.oidc.enabled=true",
        "apiml.health.protected=true",
        "apiml.security.oidc.userInfo.uri=https://localhost:/user/info"
    })
//@Import(OIDCTokenProviderEndpointTest.OidcProviderTestController.class)
@Import({GatewayOverrideConfig.class, DiscoveryClientTestConfig.class, ApimlRoutingConfig.class, OIDCTokenProviderEndpoint.class})
class OIDCTokenProviderEndpointTest {

    private final String VALID_TOKEN = "eyJ0eXAiOiJKV1QiLCJub25jZSI6ImVUbTV0TDhMeFhoUTUzVjdVbDVKSmlpZ0plNy13V1ZHUEpRVTFwelZqZ2MiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik1HTHFqOThWTkxvWGFGZnBKQ0JwZ0I0SmFLcyIsImtpZCI6Ik1HTHFqOThWTkxvWGFGZnBKQ0JwZ0I0SmFLcyJ9.eyJhdWQiOiIwMDAwMDAwMy0wMDAwLTAwMDAtYzAwMC0wMDAwMDAwMDAwMDAiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8xMTk0ZGYxNi0zYWUwLTQ5YWEtYjQ4Yi01YzRkYTZlMTM2ODkvIiwiaWF0IjoxNzIyNDI5NTI3LCJuYmYiOjE3MjI0Mjk1MjcsImV4cCI6MTcyMjQzNDgxOSwiYWNjdCI6MCwiYWNyIjoiMSIsImFjcnMiOlsidXJuOnVzZXI6cmVnaXN0ZXJzZWN1cml0eWluZm8iXSwiYWlvIjoiQVRRQXkvOFhBQUFBVzJvNGFIdFZBQnZEamFJcmVaNytuSmQ1dmJ1VGo3RWh5QjNONUZTb2xnSGNjYkNnR2FkMHJZTUdKRUVJV1QwUCIsImFtciI6WyJwd2QiXSwiYXBwX2Rpc3BsYXluYW1lIjoiTUZELU9yaW9uIiwiYXBwaWQiOiJkZTQ2ODM2My1iMGM3LTQ5YmQtYjVjYS1kYzk5OTAxNGM0Y2IiLCJhcHBpZGFjciI6IjAiLCJmYW1pbHlfbmFtZSI6IkN1bWFyYXYiLCJnaXZlbl9uYW1lIjoiQWxleGFuZHIiLCJpZHR5cCI6InVzZXIiLCJpcGFkZHIiOiIxOTIuMTkuMTUyLjIwOSIsIm5hbWUiOiJBbGV4YW5kciBDdW1hcmF2Iiwib2lkIjoiOWM0OWEwMmYtY2JkMi00NmFmLWE1MzAtOTc5YjE4MTI1NmNmIiwicGxhdGYiOiIzIiwicHVpZCI6IjEwMDMyMDAyRDMyOTExNEIiLCJyaCI6IjAuQVMwQUZ0LVVFZUE2cWttMGkxeE5wdUUyaVFNQUFBQUFBQUFBd0FBQUFBQUFBQUF0QURBLiIsInNjcCI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwic2lnbmluX3N0YXRlIjpbImttc2kiXSwic3ViIjoiVjZSWFVaRjI3WTNDUXFVdnFTcUxlTVVMTUtVUkFxU0t3RGNsemF5aGNCOCIsInRlbmFudF9yZWdpb25fc2NvcGUiOiJOQSIsInRpZCI6IjExOTRkZjE2LTNhZTAtNDlhYS1iNDhiLTVjNGRhNmUxMzY4OSIsInVuaXF1ZV9uYW1lIjoiYWM4OTMyNjZAYnJvYWRjb20ubmV0IiwidXBuIjoiYWM4OTMyNjZAYnJvYWRjb20ubmV0IiwidXRpIjoiMmJyS2d2ZzFua0NtQlRDcVhONHlBQSIsInZlciI6IjEuMCIsIndpZHMiOlsiYjc5ZmJmNGQtM2VmOS00Njg5LTgxNDMtNzZiMTk0ZTg1NTA5Il0sInhtc19pZHJlbCI6IjEgMjgiLCJ4bXNfc3QiOnsic3ViIjoiR25SemhMcGtNZmNCVFQ0dlQyYzJOVWhkYlBaOU5tckFJSnpCS0RoNUg4YyJ9LCJ4bXNfdGNkdCI6MTQwMjUwNzgwOX0.gp6CvZp0dwuSMG0yCh7g7M-LoH9SPhdy_Du3mvH1ItolXIvCHf4P993_xrnZ3pyOaxgNne7kgdLS16qy5FsBEidksAg7xtwKAwnCFNnPvgx-YJwVsak_PByj1FEcgGHnqgvtlGqmz_lB1R26Eek627Aij3ojk9tsmGn45e--tSi_jNUptjDUTlrjmSkVKmS1ae63PfYuNGTg3S2XiZE-Tt08Kyax4oQdt3G-40tCS0F-aNobwLjiDqkffu3SW4tmUuzolrTDXtlo8V4b8R5PCs0Qa3yxt5E4vLRFRiApka73Gxd8ERL9AVw-MgYx1sQjoGYAMW7jXtka4QYLQ26lDw";
  //  private final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKFZ5zZ77xw";
    private final String INVALID_TOKEN = "eyJraWQiOiJMY3hja2tvcjk0cWtydW54SFA3VGtpYjU0N3J6bWtYdnNZVi1uYzZVLU40IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULlExakp2UkZ0dUhFUFpGTXNmM3A0enQ5aHBRRHZrSU1CQ3RneU9IcTdlaEkiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2OTcwNjA3NzMsImV4cCI6MTY5NzA2NDM3MywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU5OTExOGgxNmtQT1dBbTVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjk3MDYwMDY0LCJzdWIiOiJzajg5NTA5MkBicm9hZGNvbS5uZXQiLCJncm91cHMiOlsiRXZlcnlvbmUiXX0.Cuf1JVq_NnfBxaCwiLsR5O6DBmVV1fj9utAfKWIF1hlek2hCJsDLQM4ii_ucQ0MM1V3nVE1ZatPB-W7ImWPlGz7NeNBv7jEV9DkX70hchCjPHyYpaUhAieTG75obdufiFpI55bz3qH5cPRvsKv0OKKI9T8D7GjEWsOhv6CevJJZZvgCFLGFfnacKLOY5fEBN82bdmCulNfPVrXF23rOregFjOBJ1cKWfjmB0UGWgI8VBGGemMNm3ACX3OYpTOek2PBfoCIZWOSGnLZumFTYA0F_3DsWYhIJNoFv16_EBBJcp_C0BYE_fiuXzeB0fieNUXASsKp591XJMflDQS_Zt1g";

    @Autowired
    OIDCTokenProviderEndpoint oidcTokenProviderEndpoint;

    @LocalServerPort
    protected int port;

    protected String basePath;

    @BeforeEach
    public void setBasePath() {
        basePath = String.format("https://localhost:%d", port);
    }



    @Test
    void givenValidToken_thenAccessEndpoint() {

     //   ReflectionTestUtils.setField(oidcTokenProviderEndpoint, "clock", new FixedClock(new Date(Instant.ofEpochSecond(1697060773 + 1000L).toEpochMilli())));
        given()
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN)
        .when()
           // .get(basePath + "/gateway/api/v1/services")
          //  .get(basePath + "/gateway/api/v1/conformance/gateway")
              .get(basePath + "/application/health")
        .then()
            .statusCode(is(HttpStatus.SC_OK));
    }

    @Test
    void givenInvalidToken_thenRejectAccessToEndpoint() {
        given()
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION_PREFIX + " " + INVALID_TOKEN)
        .when()
            .get(basePath + "/application/health")
        .then()
            .statusCode(is(HttpStatus.SC_UNAUTHORIZED));
    }

    @Controller
    class OidcProviderTestController {

        @GetMapping("/user/info")
        ResponseEntity<String> verify(@RequestHeader(HttpHeaders.AUTHORIZATION) String authenticationHeader) {
            if (StringUtils.equals(BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN, authenticationHeader)) {
                return ResponseEntity.ok("{\"detail\":\"information\")");
            }
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body("{\"error\":\"message\")");
        }

    }

}
