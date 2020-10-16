/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CachingControllerTest {
    private CachingController underTest;

    private Storage mockStorage;

    @BeforeEach
    void setUp() {
        mockStorage = mock(Storage.class);
        underTest = new CachingController(mockStorage);
    }

    @Test
    void givenStorageReturnsValidValue_whenRequired_thenItIsProperlyProvided() {
        when(mockStorage.read("test-service", "key")).thenReturn(new KeyValue("key", "value"));

        ResponseEntity<?> key = underTest.getKey("key");
        KeyValue result =  (KeyValue) key.getBody();
        assertThat(result.getValue(), is("value"));
    }
}
