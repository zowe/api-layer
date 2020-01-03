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

import io.swagger.annotations.Api;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Version 1 of the controller that returns a zip file.
 */
@RestController
@Api(tags = {"Other Operations"})
public class FileController {

    private final ServletContext servletContext;
    private static final String DIRECTORY = "C:\\Users\\cz670745\\myRepos\\api-layer\\discoverable-client\\src\\main\\resources";
    private static final String DEFAULT_FILE_NAME = "sample-text.zip";

    public FileController(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @GetMapping(value = "/api/v1/get-file", produces = "application/zip")
    public ResponseEntity<InputStreamResource> zipFiles(@RequestParam(defaultValue = DEFAULT_FILE_NAME) String fileName) throws FileNotFoundException {

        File file = new File(DIRECTORY + "/" + fileName);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        String mineType = servletContext.getMimeType(fileName);
        MediaType mediaType = MediaType.parseMediaType(mineType);

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
            .contentLength(file.length())
            .body(resource);

    }
}
