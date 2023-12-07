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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/")
public class ImageController {

    @Value("${apiml.catalog.customStyle.logo:}")
    private String image;

    @GetMapping(value = "/custom-logo")
    @HystrixCommand()
    @ResponseBody
    public ResponseEntity<InputStreamResource> downloadImage() {
        File imageFile = new File(image);
        try (InputStream imageStream = new FileInputStream(imageFile)) {
            String extension = image.substring(image.lastIndexOf(".") + 1);
            MediaType mediaType;
            switch (extension.toLowerCase()) {
                case "png":
                    mediaType = MediaType.IMAGE_PNG;
                    break;
                case "jpg":
                case "jpeg":
                    mediaType = MediaType.IMAGE_JPEG;
                    break;
                case "svg":
                    mediaType = MediaType.valueOf("image/svg+xml");
                    break;
                default:
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    break;
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(imageStream));
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
