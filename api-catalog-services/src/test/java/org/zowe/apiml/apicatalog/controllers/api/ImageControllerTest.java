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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ImageController imageController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(imageController).build();
    }

    @Nested
    class GivenImageEndpointRequest {
        @Nested
        class WhenPngFormat {
            @Test
            void thenDownloadImage() throws Exception {
                ReflectionTestUtils.setField(imageController, "image", "src/test/resources/api-catalog.png");

                mockMvc.perform(get("/custom-logo"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andReturn();
            }
        }

        @Nested
        class WhenJpegFormat {
            @Test
            void thenDownloadImage() throws Exception {
                ReflectionTestUtils.setField(imageController, "image", "src/test/resources/api-catalog.jpeg");

                mockMvc.perform(get("/custom-logo"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andReturn();
                ReflectionTestUtils.setField(imageController, "image", "src/test/resources/api-catalog.jpg");

                mockMvc.perform(get("/custom-logo"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andReturn();
            }
        }

        @Nested
        class WhenSvgFormat {
            @Test
            void thenDownloadImage() throws Exception {
                ReflectionTestUtils.setField(imageController, "image", "src/test/resources/api-catalog.svg");

                mockMvc.perform(get("/custom-logo"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.valueOf("image/svg+xml")))
                    .andReturn();
            }
        }

        @Test
        void thenReturnFileNotFound() throws Exception {
            ReflectionTestUtils.setField(imageController, "image", "wrong/path/img.png");

            mockMvc.perform(get("/custom-logo"))
                .andExpect(status().isNotFound());
        }
    }
}
