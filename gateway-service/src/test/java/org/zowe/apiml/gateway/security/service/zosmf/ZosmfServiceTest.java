/**
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.zosmf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZosmfServiceTest {

    private static final String ZOSMF_ID = "zosmf";

    @Mock
    private AuthConfigurationProperties authConfigurationProperties;
    private DiscoveryClient discovery = mock(DiscoveryClient.class);
    private RestTemplate restTemplate = mock(RestTemplate.class);
    @Spy
    private ObjectMapper securityObjectMapper;

    private ZosmfService getZosmfServiceSpy() {
        ZosmfService zosmfServiceObj = new ZosmfService(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
        ZosmfService zosmfService = spy(zosmfServiceObj);
        doReturn(ZOSMF_ID).when(zosmfService).getZosmfServiceId();
        doReturn("http://zosmf:1433").when(zosmfService).getURI(ZOSMF_ID);
        return zosmfService;
    }

    private HttpHeaders getBasicRequestHeaders() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        requestHeaders = addCSRFHeader(requestHeaders);
        return requestHeaders;
    }

    private HttpHeaders addCSRFHeader(HttpHeaders headers) {
        headers.add("X-CSRF-ZOSMF-HEADER", "");
        return headers;
    }

    @Test
    public void testAuthenticateShouldUseAuthEndpointWhenAuthEndpointExists() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.POST);

        HttpHeaders requestHeaders = getBasicRequestHeaders();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE, "jwtToken=jt");
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", responseHeaders, HttpStatus.OK);

        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.POST,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pass");
        ZosmfService.AuthenticationResponse response = zosmfService.authenticate(authentication);

        assertNotNull(response);
        assertNotNull(response.getTokens());
        assertEquals(1, response.getTokens().size());
        assertEquals("jt", response.getTokens().get(ZosmfService.TokenType.JWT));
    }

    @Test
    public void testAuthenticateShouldUseInfoEndpointWhenAuthEndpointDoesNotExist() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        doReturn(false).when(zosmfService).authenticationEndpointExists(HttpMethod.POST);
        doReturn("realm").when(zosmfService).getZosmfRealm("http://zosmf:1433/zosmf/info");
        HttpHeaders requestHeaders = getBasicRequestHeaders();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.SET_COOKIE, "LtpaToken2=lt");
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", responseHeaders, HttpStatus.OK);

        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/info",
            HttpMethod.GET,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pass");
        ZosmfService.AuthenticationResponse response = zosmfService.authenticate(authentication);

        assertNotNull(response);
        assertNotNull(response.getTokens());
        assertEquals(1, response.getTokens().size());
        assertEquals("lt", response.getTokens().get(ZosmfService.TokenType.LTPA));
    }

    @Test(expected = AuthenticationServiceException.class)
    public void testAuthenticeShouldThrowException() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doThrow(new RestClientException("any exception")).when(restTemplate).exchange(
            anyString(),
            (HttpMethod) any(),
            (HttpEntity<?>) any(),
            (Class<?>) any()
        );

        zosmfService.authenticate(new UsernamePasswordAuthenticationToken("user", "pass"));
    }

    @Test
    public void testAuthenticationEndpointExists() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        prepareAuthenticationEndpoint(zosmfService, responseException);
        assertTrue(zosmfService.authenticationEndpointExists(HttpMethod.POST));
    }

    @Test
    public void testAuthenticationEndpointExistsNotFoundException() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        HttpClientErrorException responseException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        prepareAuthenticationEndpoint(zosmfService, responseException);

        assertFalse(zosmfService.authenticationEndpointExists(HttpMethod.POST));
    }

    @Test
    public void testAuthenticationEndpointExistsRuntimeException() {
        ZosmfService zosmfService = getZosmfServiceSpy();

        RuntimeException responseException = new RuntimeException("Runtime Exception");
        prepareAuthenticationEndpoint(zosmfService, responseException);

        assertFalse(zosmfService.authenticationEndpointExists(HttpMethod.POST));
    }

    public void prepareAuthenticationEndpoint(ZosmfService zosmfService, Exception exception) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(zosmfService.ZOSMF_CSRF_HEADER, "");

        doThrow(exception).when(restTemplate).exchange(
            anyString(),
            (HttpMethod) any(),
            (HttpEntity<?>) any(),
            (Class<?>) any()
        );
    }

    @Test
    public void testValidateJWT() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.POST);
        setResponseForValidateTest(HttpStatus.OK, "jwtToken=jwt");
        zosmfService.validate(ZosmfService.TokenType.JWT, "jwt");
    }

    @Test
    public void testValidateLTPA() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.POST);
        setResponseForValidateTest(HttpStatus.OK, "LtpaToken2=lt");
        zosmfService.validate(ZosmfService.TokenType.LTPA, "lt");
    }

    @Test
    public void testValidateInvalidToken() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.POST);
        setResponseForValidateTest(HttpStatus.UNAUTHORIZED, "jwtToken=jwt");

        try {
            zosmfService.validate(ZosmfService.TokenType.JWT, "jwt");
        } catch (TokenNotValidException e) {
            assertEquals("Token is not valid.", e.getMessage());
        }
    }

    @Test
    public void testValidateUnexpectedHttpStatusCode() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.POST);
        setResponseForValidateTest(HttpStatus.I_AM_A_TEAPOT, "jwtToken=jwt");

        try {
            zosmfService.validate(ZosmfService.TokenType.JWT, "jwt");
        } catch (ServiceNotAccessibleException e) {
            assertEquals("Could not get an access to z/OSMF service.", e.getMessage());
        }
    }

    /**
     * Mock the rest template exchange to expect desired authToken string and set the desired response status code
     * @param responseStatusCode HttpStatus code to be returned by rest template
     * @param authTokenHeaderValue  String representing auth cookie e.g "jwtToken=jt"
     */
    public void setResponseForValidateTest(HttpStatus responseStatusCode, String authTokenHeaderValue) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, authTokenHeaderValue);

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", new HttpHeaders(), responseStatusCode);
        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.POST,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );
    }

    @Test
    public void testValidateRuntimeException() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.POST);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "jwtToken=jwt");

        RuntimeException exception = new RuntimeException("Runtime Exception");
        doThrow(exception).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.POST,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        try {
            zosmfService.validate(ZosmfService.TokenType.JWT, "jwt");
        } catch (RuntimeException e) {
            assertEquals("Runtime Exception", e.getMessage());
        }
    }

    @Test
    public void testInvalidateJWT() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.DELETE);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "jwtToken=jwt");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.OK);
        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        zosmfService.invalidate(ZosmfService.TokenType.JWT, "jwt");
    }

    @Test
    public void testInvalidateLTPA() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.DELETE);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "LtpaToken2=lt");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.OK);
        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        zosmfService.invalidate(ZosmfService.TokenType.LTPA, "lt");
    }

    @Test
    public void testInvalidateUnexpectedHttpStatusCode() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.DELETE);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "jwtToken=jwt");

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{}", new HttpHeaders(), HttpStatus.I_AM_A_TEAPOT);
        doReturn(responseEntity).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        try {
            zosmfService.invalidate(ZosmfService.TokenType.JWT, "jwt");
        } catch (ServiceNotAccessibleException e) {
            assertEquals("Could not get an access to z/OSMF service.", e.getMessage());
        }
    }

    @Test
    public void testInvalidateRuntimeException() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        doReturn(true).when(zosmfService).authenticationEndpointExists(HttpMethod.DELETE);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders = addCSRFHeader(requestHeaders);
        requestHeaders.add(HttpHeaders.COOKIE, "jwtToken=jwt");

        RuntimeException exception = new RuntimeException("Runtime Exception");
        doThrow(exception).when(restTemplate).exchange(
            "http://zosmf:1433/zosmf/services/authenticate",
            HttpMethod.DELETE,
            new HttpEntity<>(null, requestHeaders),
            String.class
        );

        try {
            zosmfService.invalidate(ZosmfService.TokenType.JWT, "jwt");
        } catch (RuntimeException e) {
            assertEquals("Runtime Exception", e.getMessage());
        }
    }
    @Test
    public void testReadTokenFromCookie() {
        assertNull(new ZosmfService(null, null, null,  null).readTokenFromCookie(null, null));
    }

    @Test
    public void testGetPublicKeys_zosm404() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        when(restTemplate.getForObject(anyString(), any()))
            .thenThrow(mock(HttpClientErrorException.NotFound.class));
        assertTrue(zosmfService.getPublicKeys().getKeys().isEmpty());
    }

    @Test
    public void testGetPublicKeys_invalidFormat() {
        ZosmfService zosmfService = getZosmfServiceSpy();
        when(restTemplate.getForObject(anyString(), any()))
            .thenReturn("invalidFormat");
        assertTrue(zosmfService.getPublicKeys().getKeys().isEmpty());
    }

    private static final String ZOSMF_PUBLIC_KEY_JSON = "{\n" +
        "    \"keys\": [\n" +
        "        {\n" +
        "            \"kty\": \"RSA\",\n" +
        "            \"e\": \"AQAB\",\n" +
        "            \"use\": \"sig\",\n" +
        "            \"kid\": \"ozG_ySMHRsVQFmN1mVBeS-WtCupY1r-K7ewben09IBg\",\n" +
        "            \"alg\": \"RS256\",\n" +
        "            \"n\": \"wRdwksGIAR2A4cHsoOsYcGp5AmQl5ZjF5xIPXeyjkaLHmNTMvjixdWso1ecVlVeg_6pIXzMRhmOvmjXjz1PLfI2GD3drmeqsStjISWdDfH_rIQCYc9wYbWIZ3bQ0wFRDaVpZ6iOZ2iNcIevvZQKNw9frJthKSMM52JtsgwrgN--Ub2cKWioU_d52SC2SfDzOdnChqlU7xkqXwKXSUqcGM92A35dJJXkwbZhAHnDy5FST1HqYq27MOLzBkChw1bJQHZtlSqkxcHPxphnnbFKQmwRVUvyC5kfBemX-7Mzp1wDogt5lGvBAf3Eq8rFxaevAke327rM7q2KqO_LDMN2J-Q\"\n" +
        "        }\n" +
        "    ]\n" +
        "}";

    @Test
    public void testGetPublicKeys_success() throws JSONException {
        ZosmfService zosmfService = getZosmfServiceSpy();
        when(restTemplate.getForObject(
            "http://zosmf:1433/jwt/ibm/api/zOSMFBuilder/jwk",
            String.class
        )).thenReturn(ZOSMF_PUBLIC_KEY_JSON);

        JSONAssert.assertEquals(ZOSMF_PUBLIC_KEY_JSON, new JSONObject(zosmfService.getPublicKeys().toString()), true);
    }
}
