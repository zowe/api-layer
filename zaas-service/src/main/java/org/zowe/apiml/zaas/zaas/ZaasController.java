/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.zaas;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.zaas.security.ticket.ApplicationNameNotFoundException;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.token.NoMainframeIdentityException;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;

import javax.management.ServiceNotFoundException;

import static org.zowe.apiml.zaas.filters.pre.ExtractAuthSourceFilter.AUTH_SOURCE_ATTR;
import static org.zowe.apiml.zaas.filters.pre.ExtractAuthSourceFilter.AUTH_SOURCE_PARSED_ATTR;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@RequiredArgsConstructor
@RestController
@RequestMapping(ZaasController.CONTROLLER_PATH)
public class ZaasController {
    public static final String CONTROLLER_PATH = "gateway/zaas";

    private final AuthSourceService authSourceService;
    private final PassTicketService passTicketService;
    private final ZosmfService zosmfService;
    private final TokenCreationService tokenCreationService;

    @PostMapping(path = "ticket", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides PassTicket for authenticated user.")
    public ResponseEntity<TicketResponse> getPassTicket(@RequestBody TicketRequest ticketRequest, @RequestAttribute(AUTH_SOURCE_PARSED_ATTR) AuthSource.Parsed authSourceParsed)
        throws IRRPassTicketGenerationException, ApplicationNameNotFoundException {

        final String applicationName = ticketRequest.getApplicationName();
        if (StringUtils.isBlank(applicationName)) {
            throw new ApplicationNameNotFoundException("ApplicationName not provided.");
        }

        String ticket = passTicketService.generate(authSourceParsed.getUserId(), applicationName);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(new TicketResponse("", authSourceParsed.getUserId(), applicationName, ticket));
    }

    @PostMapping(path = "zosmf", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides z/OSMF JWT or LTPA token for authenticated user.")
    public ResponseEntity<ZaasTokenResponse> getZosmfToken(@RequestAttribute(AUTH_SOURCE_ATTR) AuthSource authSource,
                                                           @RequestAttribute(AUTH_SOURCE_PARSED_ATTR) AuthSource.Parsed authSourceParsed) throws ServiceNotFoundException {

        ZaasTokenResponse zaasTokenResponse = zosmfService.exchangeAuthenticationForZosmfToken(authSource.getRawSource().toString(), authSourceParsed);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(zaasTokenResponse);
    }


    @PostMapping(path = "zoweJwt", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides zoweJwt for authenticated user.")
    public ResponseEntity<ZaasTokenResponse> getZoweJwt(@RequestAttribute(AUTH_SOURCE_ATTR) AuthSource authSource) {

        String token = authSourceService.getJWT(authSource);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ZaasTokenResponse.builder().cookieName(COOKIE_AUTH_NAME).token(token).build());

    }

    /**
     * Controller level exception handler for cases when NO mapping with mainframe ID exists.
     *
     * @param authSource credentials that will be used for authentication translation
     * @param nmie       exception thrown in case of missing user mapping
     * @return status code OK, header name and value if OIDC token is valid, otherwise status code UNAUTHORIZED
     */
    @ExceptionHandler(NoMainframeIdentityException.class)
    public ResponseEntity<ZaasTokenResponse> handleNoMainframeIdException(@RequestAttribute(AUTH_SOURCE_ATTR) AuthSource authSource, NoMainframeIdentityException nmie) {
        if (nmie.isValidToken() && authSource.getType() == AuthSource.AuthSourceType.OIDC) {
            return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ZaasTokenResponse.builder().headerName(ApimlConstants.HEADER_OIDC_TOKEN).token(String.valueOf(authSource.getRawSource())).build());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping(path = "safIdt", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides SAF Identity Token for authenticated user.")
    public ResponseEntity<ZaasTokenResponse> getSafIdToken(@RequestBody TicketRequest ticketRequest, @RequestAttribute(AUTH_SOURCE_PARSED_ATTR) AuthSource.Parsed authSourceParsed)
        throws IRRPassTicketGenerationException, ApplicationNameNotFoundException {

        final String applicationName = ticketRequest.getApplicationName();
        if (StringUtils.isBlank(applicationName)) {
            throw new ApplicationNameNotFoundException("ApplicationName not provided.");
        }

        String safIdToken = tokenCreationService.createSafIdTokenWithoutCredentials(authSourceParsed.getUserId(), applicationName);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ZaasTokenResponse.builder().token(safIdToken).build());
    }

}
