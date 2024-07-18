/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.caching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class CachingServiceClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    private CachingServiceClient client;

    @BeforeEach
    void setUp () {
        client = new CachingServiceClient();
    }

    @Nested
    class GivenCachingServiceClient {

        @Nested
        class WhenCreate {

            @Test
            void andServerSuccess_thenSuccess() {

                StepVerifier.create(client.create(new KeyValue("null", "null")))

            }

            @Test
            void andServerError_thenError() {

            }

        }

        @Nested
        class WhenDelete {

        }

        @Nested
        class WhenRead {

            @Test
            void andServerSuccess_thenSuccessAndContent() {

            }

            @Test
            void andServerError_thenError() {

            }

            @Test
            void andNotFound_thenEmpty() {

            }

        }

        @Nested
        class WhenUpdate {

            @Test
            void andServerSuccess_thenSucess() {

            }

            @Test
            void andServerError_thenError() {

            }

        }

    }

}
