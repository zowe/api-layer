/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.eureka.DomainAllowListMetadataException;
import org.zowe.apiml.registry.RegistrationRejectedException;

/**
 * Turns registration failures into responses.
 * <p>
 * Replaces the JAX-RS {@code ExceptionMapper}s and the {@code JerseyExceptionMapperRegistrar} that had to inject
 * them into Jersey's {@code ResourceConfig} through a {@code BeanPostProcessor}. With the surface served by
 * Spring MVC this is just a {@code @RestControllerAdvice}.
 * <p>
 * Scoped to the registry controller so it cannot swallow exceptions from the rest of the application.
 */
// Scoped by package rather than by type so it covers the reactive adapter too, which lives in the same
// package but in the apiml module - naming that class here would drag reactor onto this module's classpath.
@RestControllerAdvice(basePackages = "org.zowe.apiml.discovery.registry")
@RequiredArgsConstructor
@Slf4j
public class RegistryExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(DomainAllowListMetadataException.class)
    public ResponseEntity<ApiMessageView> handleDisallowedDomain(DomainAllowListMetadataException exception) {
        log.debug("Metadata validation exception: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageService.createMessage("org.zowe.apiml.common.metadataNotAllowedInRegistration").mapToView());
    }

    @ExceptionHandler(RegistrationRejectedException.class)
    public ResponseEntity<ApiMessageView> handleRejectedRegistration(RegistrationRejectedException exception) {
        log.debug("Registration rejected: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageService.createMessage("org.zowe.apiml.common.metadataNotAllowedInRegistration").mapToView());
    }

    /**
     * A malformed registration body.
     * <p>
     * 400 rather than 500: the payload is the client's problem, and returning 500 would make an enabler retry a
     * request that can never succeed.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> handleMalformedPayload(IllegalArgumentException exception) {
        log.debug("Malformed registry payload: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

}
