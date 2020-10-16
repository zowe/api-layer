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

import java.util.ArrayList;
import java.util.List;

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
    void testData() {
        List<KeyValue> results = new ArrayList<>();
        results.add(new KeyValue("key", "value"));
        when(mockStorage.read(new String[]{"key"})).thenReturn(results);

        ResponseEntity key = underTest.getKey("key");
        List result =  (List) key.getBody();
        assertThat(result.size(), is(1));
    }
}
