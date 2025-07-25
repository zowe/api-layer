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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = ImageControllerMicroservice.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@ContextConfiguration(classes = ImageControllerMicroservice.class)
class ImageControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ImageControllerMicroservice imageController;

    @Nested
    class GivenImageEndpointRequest {

        @Nested
        class WhenPngFormat {
            @Test
            void thenDownloadImage() {
                ReflectionTestUtils.setField(imageController, "image", "src/test/resources/api-catalog.png");

                webTestClient.get().uri("/apicatalog/custom-logo").exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.IMAGE_PNG);
            }
        }

        @Nested
        class WhenJpegFormat {
            @Test
            void thenDownloadImage() {
                ReflectionTestUtils.setField(imageController, "image", "src/test/resources/api-catalog.jpeg");

                webTestClient.get().uri("/apicatalog/custom-logo").exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.IMAGE_JPEG);
                ReflectionTestUtils.setField(imageController, "image", "src/test/resources/api-catalog.jpg");

                webTestClient.get().uri("/apicatalog/custom-logo").exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.IMAGE_JPEG);
            }
        }

        @Nested
        class WhenSvgFormat {
            @Test
            void thenDownloadImage() {
                ReflectionTestUtils.setField(imageController, "image", "src/test/resources/api-catalog.svg");

                webTestClient.get().uri("/apicatalog/custom-logo").exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.valueOf("image/svg+xml"));
            }
        }

        @Test
        void thenReturnFileNotFound() {
            ReflectionTestUtils.setField(imageController, "image", "wrong/path/img.png");

            webTestClient.get().uri("/apicatalog/custom-logo").exchange()
                .expectStatus().isNotFound();
        }

    }

}
