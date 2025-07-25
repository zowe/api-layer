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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.File;

public class ImageController {

    @Value("${apiml.catalog.customStyle.logo:}")
    private String image;

    private String getExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex < 0) return "";
        return fileName.substring(lastIndex + 1);
    }

    private MediaType getMediaType(String fileName) {
        switch (getExtension(fileName).toLowerCase()) {
            case "png":
                return MediaType.IMAGE_PNG;
            case "jpg":
            case "jpeg":
                return  MediaType.IMAGE_JPEG;
            case "svg":
                return MediaType.valueOf("image/svg+xml");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @GetMapping(value = "/custom-logo")
    @ResponseBody
    public Mono<ResponseEntity<FileSystemResource>> downloadImage() {
        File imageFile = new File(image);
        if (!imageFile.exists()) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(image));
        return Mono.fromSupplier(() -> ResponseEntity.ok()
            .headers(headers)
            .body(new FileSystemResource(imageFile)));
    }

}

@RestController
@RequestMapping("/apicatalog/api/v1/")
@ConditionalOnBean(name = "modulithConfig")
class ImageControllerModulith extends ImageController {

}

@RestController
@RequestMapping("/apicatalog")
@ConditionalOnMissingBean(name = "modulithConfig")
class ImageControllerMicroservice extends ImageController {

}
