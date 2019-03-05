/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.controllers.handlers;

import com.ca.mfaas.apicatalog.controllers.api.CatalogApiDocController;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.apicatalog.services.status.model.ServiceNotFoundException;
import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.rest.response.ApiMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This class creates responses for exceptional behavior of the CatalogApiDocController
 */
@ControllerAdvice(assignableTypes = {CatalogApiDocController.class})
public class CatalogApiDocControllerExceptionHandler {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CatalogApiDocControllerExceptionHandler.class);
    private final ErrorService errorService;

    /**
     * Constructor for {@link CatalogApiDocControllerExceptionHandler}.
     *
     * @param errorService service for creation {@link ApiMessage} by key and list of parameters.
     */
    @Autowired
    public CatalogApiDocControllerExceptionHandler(ErrorService errorService) {
        this.errorService = errorService;
    }

    /**
     * Could not retrieve the API Documentation
     *
     * @param exception InvalidFormatException
     * @return 500 and the message 'TBD'
     */
    @ExceptionHandler(ApiDocNotFoundException.class)
    public ResponseEntity<ApiMessage> handleApiDocNotFoundException(ApiDocNotFoundException exception) {
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.caapicatalog.apiDocNotFound", exception.getMessage());

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(message);
    }

    /**
     * Could not retrieve the API Documentation as the Gateway was not available
     *
     * @param exception InvalidFormatException
     * @return 404 and the message 'TBD'
     */
    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ApiMessage> handleServiceNotFoundException(ServiceNotFoundException exception) {

        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.caapicatalog.serviceNotFound", exception.getMessage());

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(message);
    }
}
