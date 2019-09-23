/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.api;

import com.ca.mfaas.client.exception.PetIdMismatchException;
import com.ca.mfaas.client.exception.PetNotFoundException;
import com.ca.mfaas.message.api.ApiMessage;
import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.core.Message;
import com.ca.mfaas.message.core.MessageService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
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
import java.util.stream.Collectors;

/**
 * This class creates responses for exceptional behavior of the PetController
 */
@ControllerAdvice(assignableTypes = {PetController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PetControllerExceptionHandler {

    private final MessageService messageService;

    /**
     * Constructor for {@link PetControllerExceptionHandler}.
     * @param messageService service for creation {@link Message} by key and list of parameters.
     */
    @Autowired
    public PetControllerExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }
    /**
     * The handlePetNotFound method creates a response when the pet with a provided ID is not found
     * @param exception PetNotFoundException
     * @return 404 and the message 'Pet with provided ID not found'
     */
    @ExceptionHandler(PetNotFoundException.class)
    public ResponseEntity<ApiMessageView> handlePetNotFound(PetNotFoundException exception) {
        Message message = messageService.createMessage("com.ca.mfaas.api.sampleservice.petNotFound", exception.getId());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message.mapToView());
    }

    /**
     * The handleIdMismatch method creates a response when the pet ID in the request body and the pet ID in the URL are different
     * @param exception PetIdMismatchException
     * @return 400 and the message 'Invalid ID'
     */
    @ExceptionHandler(PetIdMismatchException.class)
    public ResponseEntity<ApiMessageView> handleIdMismatch(PetIdMismatchException exception) {
        Message message = messageService.createMessage("com.ca.mfaas.api.sampleservice.petIdMismatchException", exception.getPathId(), exception.getBodyId());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message.mapToView());
    }

    /**
     * The handleIdTypeMismatch method creates a response when the pet ID is invalid
     * @param exception TypeMismatchException
     * @return 400 and the message 'The pet ID is invalid: it is not an integer'
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiMessageView> handleIdTypeMismatch(TypeMismatchException exception) {
        Message message = messageService.createMessage("com.ca.mfaas.api.sampleservice.petIdTypeMismatch", exception.getValue());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message.mapToView());
    }

    /**
     * The handleMethodArgumentNotValid method creates a response with a list of messages that contains the fields with errors
     * @param exception MethodArgumentNotValidException
     * @return 400 and a list of messages with invalid fields
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessageView> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
        List<Object[]> messages = new ArrayList<>();

        for (FieldError fieldError : fieldErrors) {
            Object[] messageFields = new Object[3];
            messageFields[0] = fieldError.getField();
            messageFields[1] = fieldError.getRejectedValue();
            messageFields[2] = fieldError.getDefaultMessage();
            messages.add(messageFields);
        }

        List<ApiMessage> listApiMessage = messageService
            .createMessage("com.ca.mfaas.api.sampleservice.petMethodArgumentNotValid", messages)
            .stream()
            .map(Message::mapToApiMessage)
            .collect(Collectors.toList());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(new ApiMessageView(listApiMessage));
    }

    /**
     * The handleUnrecognizedProperty method creates a response when the request body does not correspond to the model object
     * @param exception UnrecognizedPropertyException
     * @return 400 and the message 'Unrecognized field '%s''
     */
    @ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<ApiMessageView> handleUnrecognizedProperty(UnrecognizedPropertyException exception) {
        Message message = messageService.createMessage("com.ca.mfaas.api.sampleservice.petUnrecognizedProperty", exception.getPropertyName());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message.mapToView());
    }

    /**
     * The jsonParseException method creates a response when the provided body is not a valid JSON
     * @param exception JsonParseException
     * @return 400 and the message 'Request is not valid JSON'
     */
    @ExceptionHandler(JsonParseException.class)
    public ResponseEntity<ApiMessageView> jsonParseException(JsonParseException exception) {
        Message message = messageService.createMessage("com.ca.mfaas.api.sampleservice.jsonParseException", exception.getMessage());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message.mapToView());
    }

    /**
     * The handleInvalidFormatException method creates a response when the field is in the wrong format
     * @param exception InvalidFormatException
     * @return 400 and the message 'Field name has wrong format'
     */
    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ApiMessageView> handleInvalidFormatException(InvalidFormatException exception) {
        String fieldName = exception.getPath().get(0).getFieldName();
        Message message = messageService.createMessage("com.ca.mfaas.api.sampleservice.petInvalidFormatException", fieldName);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(message.mapToView());
    }
}
