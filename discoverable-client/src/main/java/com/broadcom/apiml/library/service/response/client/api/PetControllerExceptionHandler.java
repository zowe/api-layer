/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.client.api;

import com.broadcom.apiml.library.service.response.client.exception.PetIdMismatchException;
import com.broadcom.apiml.library.service.response.client.exception.PetNotFoundException;
import com.broadcom.apiml.library.service.response.util.MessageCreationService;
import com.broadcom.apiml.library.response.ApiMessage;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * This class creates responses for exceptional behavior of the PetController
 */
@ControllerAdvice(assignableTypes = {PetController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PetControllerExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PetControllerExceptionHandler.class);

    private final MessageCreationService errorService;

    /**
     * Constructor for {@link PetControllerExceptionHandler}.
     *
     * @param errorService service for creation {@link ApiMessage} by key and list of parameters.
     */
    @Autowired
    public PetControllerExceptionHandler(MessageCreationService errorService) {
        this.errorService = errorService;
    }

    /**
     * The handlePetNotFound method creates a message when the pet with a provided ID is not found
     *
     * @param exception PetNotFoundException
     * @return 404 and the message 'Pet with provided ID not found'
     */
    @ExceptionHandler(PetNotFoundException.class)
    public ResponseEntity<ApiMessage> handlePetNotFound(PetNotFoundException exception) {
        LOGGER.debug("Pet with id:[{}] not found", exception.getId());
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.sampleservice.petNotFound", exception.getId());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message);
    }

    /**
     * The handleIdMismatch method creates a message when the pet ID in the request body and the pet ID in the URL are different
     *
     * @param exception PetIdMismatchException
     * @return 400 and the message 'Invalid ID'
     */
    @ExceptionHandler(PetIdMismatchException.class)
    public ResponseEntity<ApiMessage> handleIdMismatch(PetIdMismatchException exception) {
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.sampleservice.petIdMismatchException", exception.getPathId(), exception.getBodyId());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message);
    }

    /**
     * The handleIdTypeMismatch method creates a message when the pet ID is invalid
     *
     * @param exception TypeMismatchException
     * @return 400 and the message 'The pet ID is invalid: it is not an integer'
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiMessage> handleIdTypeMismatch(TypeMismatchException exception) {
        String invalidIdValue = String.valueOf(exception.getValue());
        LOGGER.debug("Invalid user input for pet id:[{}]", invalidIdValue);
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.sampleservice.petIdTypeMismatch", exception.getValue());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message);
    }

    /**
     * The handleMethodArgumentNotValid method creates a message with a list of messages that contains the fields with errors
     *
     * @param exception MethodArgumentNotValidException
     * @return 400 and a list of messages with invalid fields
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessage> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        List<Object[]> messages = new ArrayList<>();

        for (FieldError fieldError : fieldErrors) {
            Object[] messageFields = new Object[3];
            messageFields[0] = fieldError.getField();
            messageFields[1] = fieldError.getRejectedValue();
            messageFields[2] = fieldError.getDefaultMessage();
            messages.add(messageFields);
        }

        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.sampleservice.petMethodArgumentNotValid", messages);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message);
    }

    /**
     * The handleUnrecognizedProperty method creates a message when the request body does not correspond to the model object
     *
     * @param exception UnrecognizedPropertyException
     * @return 400 and the message 'Unrecognized field '%s''
     */
    @ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<ApiMessage> handleUnrecognizedProperty(UnrecognizedPropertyException exception) {
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.sampleservice.petUnrecognizedProperty", exception.getPropertyName());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message);
    }

    /**
     * The jsonParseException method creates a message when the provided body is not a valid JSON
     *
     * @param exception JsonParseException
     * @return 400 and the message 'Request is not valid JSON'
     */
    @ExceptionHandler(JsonParseException.class)
    public ResponseEntity<ApiMessage> jsonParseException(JsonParseException exception) {
        LOGGER.debug("Invalid JSON request: {}", exception.getMessage());
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.sampleservice.jsonParseException", exception.getMessage());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message);
    }

    /**
     * The handleInvalidFormatException method creates a message when the field is in the wrong format
     *
     * @param exception InvalidFormatException
     * @return 400 and the message 'Field name has wrong format'
     */
    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ApiMessage> handleInvalidFormatException(InvalidFormatException exception) {
        String fieldName = exception.getPath().get(0).getFieldName();
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.sampleservice.petInvalidFormatException", fieldName);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message);
    }
}
