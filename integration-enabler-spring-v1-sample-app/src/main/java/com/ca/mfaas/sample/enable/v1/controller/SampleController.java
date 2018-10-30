/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.sample.enable.v1.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Api(tags = {"V1EnablerSampleApp"})
public class SampleController {

    @GetMapping(value = "/samples", produces = "application/json")
    @ApiOperation(value = "Retrieve all samples",
        notes = "Simple method to demonstrate how to expose an API endpoint with Open API information",
        responseContainer = "List",
        response = Sample.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "URI not found"),
        @ApiResponse(code = 500, message = "Internal Error"),
    })
    public ResponseEntity<List<Sample>> list() {
        List<Sample> samples;
        try {
            samples = new ArrayList<>();
            samples.add(new Sample("one", "first one", 1));
            samples.add(new Sample("two", "second one", 2));
            samples.add(new Sample("three", "third one", 3));
            samples.add(new Sample("four", "fourth one", 4));
            return new ResponseEntity<>(samples, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Something bad happened: " + e.getMessage(), e);
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
