/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service.inmemory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.model.KeyValue;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InMemoryStorageTest {
    private InMemoryStorage underTest;

    @BeforeEach
    void setUp() {
        underTest = new InMemoryStorage();
    }

    @Test
    void testReadValue() {
        List<KeyValue> stored = underTest.read(new String[]{"key"});
        assertThat(stored.size(), is(1));
    }
}
