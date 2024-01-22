/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zowe.apiml.apicatalog.standalone.ExampleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This controller simulates the responses of services that are not available in the standalone mode.
 */
@Controller
@RequestMapping("/mock")
@SuppressWarnings("squid:S3752") // this controller cannot be more accurate in definition, it should handle all requests
@Tag(name = "API Catalog")
@RequiredArgsConstructor
@ConditionalOnProperty(value = "apiml.catalog.standalone.enabled", havingValue = "true")
public class MockController {

    private final ExampleService exampleService;

    @RequestMapping("/**")
    public void mockEndpoint(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        String path = httpServletRequest.getRequestURI();
        int index = path.indexOf("/mock");
        if (index >= 0) path = path.substring(index + "/mock".length());
        exampleService.replyExample(httpServletResponse, httpServletRequest.getMethod(), path);
    }

}
