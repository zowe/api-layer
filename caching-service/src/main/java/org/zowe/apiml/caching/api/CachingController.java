/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.api;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.caching.exceptions.CachingPayloadException;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CachingController {
    //TODO hash key values to adjust for limit of 250 chars, and only use ascii alphanum

    private static final String TOKEN_COOKIE_PREFIX = "apimlAuthenticationToken";

    private final Storage storage;
    private final ZaasClient zaasClient;
    private final MessageService messageService;

    @GetMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves a specific value in the cache",
        notes = "Value returned is for the provided {key}")
    @ResponseBody
    public ResponseEntity<?> getValue(@PathVariable String key, HttpServletRequest request) {
        ZaasToken token;
        try {
            token = queryTokenFromRequest(request);
        } catch (ZaasClientException e) {
            return handleZaasClientException(e, request);
        }

        String serviceId = token.getUserId();

        if (key == null) {
            return noKeyProvidedResponse(serviceId);
        }

        KeyValue readPair = storage.read(serviceId, key);

        if (readPair == null) {
            Message message = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", key, serviceId);
            return new ResponseEntity<>(message.mapToView(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(readPair, HttpStatus.OK);
    }

    @GetMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves all values in the cache",
        notes = "Values returned for the calling service")
    @ResponseBody
    public ResponseEntity<?> getAllValues(HttpServletRequest request) {
        ZaasToken token;
        try {
            token = queryTokenFromRequest(request);
        } catch (ZaasClientException e) {
            return handleZaasClientException(e, request);
        }

        String serviceId = token.getUserId();
        return new ResponseEntity<>(storage.readForService(serviceId), HttpStatus.OK);
    }

    @PostMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Create a new key in the cache",
        notes = "A new key-value pair will be added to the cache")
    @ResponseBody
    public ResponseEntity<?> createKey(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        ZaasToken token;
        try {
            token = queryTokenFromRequest(request);
        } catch (ZaasClientException e) {
            return handleZaasClientException(e, request);
        }

        try {
            checkForInvalidPayload(keyValue);
        } catch (CachingPayloadException e) {
            return invalidPayloadResponse(e, keyValue);
        }

        String serviceId = token.getUserId();
        KeyValue createdPair = storage.create(serviceId, keyValue);

        if (createdPair == null) {
            Message message = messageService.createMessage("org.zowe.apiml.cache.keyCollision", keyValue.getKey());
            return new ResponseEntity<>(message.mapToView(), HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping(value = "/cache", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Update key in the cache",
        notes = "Value at the key in the provided key-value pair will be updated to the provided value")
    @ResponseBody
    public ResponseEntity<?> update(@RequestBody KeyValue keyValue, HttpServletRequest request) {
        ZaasToken token;
        try {
            token = queryTokenFromRequest(request);
        } catch (ZaasClientException e) {
            return handleZaasClientException(e, request);
        }

        try {
            checkForInvalidPayload(keyValue);
        } catch (CachingPayloadException e) {
            return invalidPayloadResponse(e, keyValue);
        }

        String serviceId = token.getUserId();
        KeyValue updatedPair = storage.update(serviceId, keyValue);

        if (updatedPair == null) {
            Message message = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", keyValue.getKey(), serviceId);
            return new ResponseEntity<>(message.mapToView(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(value = "/cache/{key}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Delete key from the cache",
        notes = "Will delete key-value pair for the provided {key}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable String key, HttpServletRequest request) {
        ZaasToken token;
        try {
            token = queryTokenFromRequest(request);
        } catch (ZaasClientException e) {
            return handleZaasClientException(e, request);
        }

        String serviceId = token.getUserId();

        if (key == null) {
            return noKeyProvidedResponse(serviceId);
        }

        KeyValue deletedPair = storage.delete(serviceId, key);

        if (deletedPair == null) {
            Message message = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", key, serviceId);
            return new ResponseEntity<>(message.mapToView(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void checkForInvalidPayload(KeyValue keyValue) throws CachingPayloadException {
        if (keyValue == null) {
            throw new CachingPayloadException("No KeyValue provided in the payload");
        }

        if (keyValue.getValue() == null) {
            throw new CachingPayloadException("No value provided in the payload");
        }

        String key = keyValue.getKey();
        if (key != null && !StringUtils.isAlphanumeric(key)) {
            throw new CachingPayloadException("Key is not alphanumeric");
        }
    }

    private ResponseEntity<ApiMessageView> invalidPayloadResponse(CachingPayloadException e, KeyValue keyValue) {
        Message message = messageService.createMessage("org.zowe.apiml.cache.invalidPayload", keyValue, e.getMessage());
        return new ResponseEntity<>(message.mapToView(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ApiMessageView> noKeyProvidedResponse(String serviceId) {
        Message message = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided", serviceId);
        return new ResponseEntity<>(message.mapToView(), HttpStatus.BAD_REQUEST);
    }

    private ZaasToken queryTokenFromRequest(HttpServletRequest request) throws ZaasClientException {
        String jwtToken = getJwtTokenFromCookie(request);
        ZaasToken zaasToken = zaasClient.query(jwtToken);

        if (zaasToken == null) {
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN, "Queried token is null");
        }
        if (zaasToken.isExpired()) {
            throw new ZaasClientException(ZaasClientErrorCodes.EXPIRED_JWT_EXCEPTION, "Queried token is expired");
        }

        return zaasToken;
    }

    private String getJwtTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
            .filter(cookie -> cookie.getName().equals(TOKEN_COOKIE_PREFIX))
            .filter(cookie -> !cookie.getValue().isEmpty())
            .findFirst()
            .map(Cookie::getValue)
            .orElse(null);
    }

    private ResponseEntity<ApiMessageView> handleZaasClientException(ZaasClientException e, HttpServletRequest request) {
        String requestUrl = request.getRequestURL().toString();
        Message message;
        HttpStatus statusCode;

        switch (e.getErrorCode()) {
            case TOKEN_NOT_PROVIDED:
                statusCode = HttpStatus.BAD_REQUEST;
                message = messageService.createMessage("org.zowe.apiml.security.query.tokenNotProvided", requestUrl);
                break;
            case INVALID_JWT_TOKEN:
                statusCode = HttpStatus.UNAUTHORIZED;
                message = messageService.createMessage("org.zowe.apiml.security.query.invalidToken", requestUrl);
                break;
            case EXPIRED_JWT_EXCEPTION:
                statusCode = HttpStatus.UNAUTHORIZED;
                message = messageService.createMessage("org.zowe.apiml.security.expiredToken", requestUrl);
                break;
            case SERVICE_UNAVAILABLE:
                statusCode = HttpStatus.NOT_FOUND;
                message = messageService.createMessage("org.zowe.apiml.cache.gatewayUnavailable", requestUrl, e.getMessage());
                break;
            default:
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
                message = messageService.createMessage("org.zowe.apiml.common.internalRequestError", requestUrl, e.getMessage(), e.getCause());
                break;
        }

        return new ResponseEntity<>(message.mapToView(), statusCode);
    }
}
