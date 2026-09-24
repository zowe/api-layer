/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.zowe.apiml.client.exception.PetIdMismatchException;
import org.zowe.apiml.client.exception.PetNotFoundException;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;

import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class creates responses for exceptional behavior of the PetController
 */
@ControllerAdvice(assignableTypes = {PetController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PetControllerExceptionHandler {
    private final MessageService messageService;

    /**
     * The handlePetNotFound method creates a response when the pet with a provided ID is not found
     *
     * @param exception PetNotFoundException
     * @return 404 and the message 'Pet with provided ID not found'
     */
    @ExceptionHandler(PetNotFoundException.class)
    public ResponseEntity<ApiMessageView> handlePetNotFound(PetNotFoundException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.sampleservice.api.petNotFound", exception.getId());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(message.mapToView());
    }

    /**
     * The handleIdMismatch method creates a response when the pet ID in the request body and the pet ID in the URL are different
     *
     * @param exception PetIdMismatchException
     * @return 400 and the message 'Invalid ID'
     */
    @ExceptionHandler(PetIdMismatchException.class)
    public ResponseEntity<ApiMessageView> handleIdMismatch(PetIdMismatchException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.sampleservice.api.petIdMismatchException", exception.getPathId(), exception.getBodyId());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(message.mapToView());
    }

    /**
     * The handleIdTypeMismatch method creates a response when the pet ID is invalid
     *
     * @param exception TypeMismatchException
     * @return 400 and the message 'The pet ID is invalid: it is not an integer'
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiMessageView> handleIdTypeMismatch(TypeMismatchException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.sampleservice.api.petIdTypeMismatch", exception.getValue());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(message.mapToView());
    }

    /**
     * The handleMethodArgumentNotValid method creates a response with a list of messages that contains the fields with errors
     *
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
            .createMessage("org.zowe.apiml.sampleservice.api.petMethodArgumentNotValid", messages)
            .stream()
            .map(Message::mapToApiMessage)
            .toList();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ApiMessageView(listApiMessage));
    }

    /**
     * The handleUnrecognizedProperty method creates a response when the request body does not correspond to the model object
     *
     * @param exception UnrecognizedPropertyException
     * @return 400 and the message 'Unrecognized field '%s''
     */
    @ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<ApiMessageView> handleUnrecognizedProperty(UnrecognizedPropertyException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.sampleservice.api.petUnrecognizedProperty", exception.getPropertyName());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(message.mapToView());
    }

    /**
     * Spring Boot 4 surfaces Jackson binding failures as HttpMessageNotReadableException with the Jackson
     * exception as the cause, rather than letting the Jackson exception itself reach the handler methods.
     * Without this, the specific handlers below are never selected and the client got a bare 400 with an
     * empty body instead of the ApiMessageView the API promises.
     *
     * @param exception the wrapper thrown by the message converter
     * @return 400 with the message for the underlying Jackson failure
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiMessageView> handleNotReadable(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof UnrecognizedPropertyException unrecognized) {
            return handleUnrecognizedProperty(unrecognized);
        }
        if (cause instanceof InvalidFormatException invalidFormat) {
            return handleInvalidFormatException(invalidFormat);
        }
        Throwable message = messageForUnreadable(exception);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageService.createMessage("org.zowe.apiml.sampleservice.api.jsonParseException", message.getMessage()).mapToView());
    }

    private Throwable messageForUnreadable(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        return cause == null ? exception : cause;
    }

    /**
     * The jsonParseException method creates a response when the provided body is not a valid JSON
     *
     * @param exception StreamReadException (Jackson 3's replacement for JsonParseException)
     * @return 400 and the message 'Request is not valid JSON'
     */
    @ExceptionHandler(StreamReadException.class)
    public ResponseEntity<ApiMessageView> jsonParseException(StreamReadException exception) {
        Message message = messageService.createMessage("org.zowe.apiml.sampleservice.api.jsonParseException", exception.getMessage());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(message.mapToView());
    }

    /**
     * The handleInvalidFormatException method creates a response when the field is in the wrong format
     *
     * @param exception InvalidFormatException
     * @return 400 and the message 'Field name has wrong format'
     */
    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<ApiMessageView> handleInvalidFormatException(InvalidFormatException exception) {
        String fieldName = exception.getPath().get(0).getPropertyName();
        Message message = messageService.createMessage("org.zowe.apiml.sampleservice.api.petInvalidFormatException", fieldName);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(message.mapToView());
    }
}
