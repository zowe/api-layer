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

import org.zowe.apiml.client.model.UploadFileResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@RestController
public class MultipartController {
    @PostMapping(
        value = "api/v1/multipart",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @HystrixCommand
    public UploadFileResponse uploadFileWithPost(@RequestParam("file") MultipartFile file) {
        return new UploadFileResponse(file.getOriginalFilename(), file.getContentType(), file.getSize());
    }

    @PutMapping(
        value = "api/v1/multipart",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @HystrixCommand
    public UploadFileResponse uploadFileWithPut(@RequestParam("file") MultipartFile file) {
        return new UploadFileResponse(file.getOriginalFilename(), file.getContentType(), file.getSize());
    }
}
