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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.caching.model.KeyValue;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingServiceClientApiTest {

    public static final String LB_USER_SERVICE = "lb.user:service";
    @Mock
    Storage storage;

    @InjectMocks
    CachingServiceClientApi client;

    CachingServiceClient.ApiKeyValue sampleKv = new CachingServiceClient.ApiKeyValue(LB_USER_SERVICE, "value");
    KeyValue mappedKv = new KeyValue(LB_USER_SERVICE, "value");

    @Nested
    class CreateTests {

        @Test
        void success() throws StorageException {

            StepVerifier.create(client.create(sampleKv))
                .verifyComplete();

            ArgumentCaptor<KeyValue> captor = ArgumentCaptor.forClass(KeyValue.class);
            verify(storage).create(eq("service"), captor.capture());

            KeyValue captured = captor.getValue();
            assertEquals(sampleKv.getKey(), captured.getKey());
            assertEquals(sampleKv.getValue(), captured.getValue());
        }

    }

    @Nested
    class UpdateTests {

        @Test
        void success() throws StorageException {

            StepVerifier.create(client.update(sampleKv))
                .verifyComplete();

            ArgumentCaptor<KeyValue> captor = ArgumentCaptor.forClass(KeyValue.class);
            verify(storage).update(eq("service"), captor.capture());

            KeyValue captured = captor.getValue();
            assertEquals(LB_USER_SERVICE, captured.getKey());
            assertEquals("value", captured.getValue());
        }

    }

    @Nested
    class ReadTests {

        @Test
        void success_thenReturnValue() throws StorageException {
            when(storage.read("service", LB_USER_SERVICE)).thenReturn(mappedKv);

            StepVerifier.create(client.read(LB_USER_SERVICE))
                .expectNext(sampleKv)
                .verifyComplete();
        }

        @Test
        void notFound_thenReturnEmpty() throws StorageException {
            when(storage.read("service", LB_USER_SERVICE)).thenReturn(null);

            StepVerifier.create(client.read(LB_USER_SERVICE))
                .verifyComplete();
        }

    }

    @Nested
    class DeleteTests {

        @Test
        void success() throws StorageException {
            StepVerifier.create(client.delete(LB_USER_SERVICE))
                .verifyComplete();

            verify(storage).delete("service", LB_USER_SERVICE);
        }

    }
}
