/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.eureka.DomainAllowListMetadataException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainAllowListExceptionMapper implements ExceptionMapper<DomainAllowListMetadataException> {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Override
    public Response toResponse(DomainAllowListMetadataException exception) {
        log.debug("Metadata validation exception: {}", exception.getMessage());
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.common.metadataNotAllowedInRegistration").mapToView();
        try {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(objectMapper.writeValueAsString(messageView))
                .build();
        } catch (JsonProcessingException e) {
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(e.getMessage())
                .build();
        }

    }

}
