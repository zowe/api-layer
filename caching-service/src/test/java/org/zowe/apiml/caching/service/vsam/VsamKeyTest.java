/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.config.VsamConfig;
import org.zowe.apiml.caching.model.KeyValue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VsamKeyTest {

    VsamConfig config;

    @BeforeEach
    void prepareConfig() {
        config = mock(VsamConfig.class);
        when(config.getKeyLength()).thenReturn(30);
    }

    @Test
    void canGetInformationAboutKey() {

        VsamKey underTest = new VsamKey(config);
        assertThat(underTest.getKeyLength(), is(config.getKeyLength()));
    }

    @Test
    void canGetKey() {
        VsamKey underTest = new VsamKey(config);

        String serviceId = "gateway";
        String key = "apiml.service.name";
        assertThat(underTest.getKey(serviceId, key).length(), is(config.getKeyLength()));
        assertThat(underTest.getKey(serviceId, key), containsString(String.valueOf(serviceId.hashCode())));
        assertThat(underTest.getKey(serviceId, key), containsString(":"));
        assertThat(underTest.getKey(serviceId, key), containsString(String.valueOf(key.hashCode())));
    }

    @Test
    void canGetKeyWithJustTheSid() {
        VsamKey underTest = new VsamKey(config);

        String serviceId = "gateway";
        String key = "apiml.service.name";
        assertThat(underTest.getKeySidOnly(serviceId).length(), is(config.getKeyLength()));
        assertThat(underTest.getKeySidOnly(serviceId), containsString(String.valueOf(serviceId.hashCode())));
        assertThat(underTest.getKeySidOnly(serviceId), not(containsString(":")));
        assertThat(underTest.getKeySidOnly(serviceId), not(containsString(String.valueOf(key.hashCode()))));

    }

    @Test
    void canGetKeyFromKeyValue() {
        KeyValue kv = new KeyValue("key", "value");
        String serviceId = "serviceId";
        VsamKey underTest = new VsamKey(config);

        assertThat(underTest.getKey(serviceId, kv), notNullValue());
    }

    @Test
    void hasMinimalLength() {
        when(config.getKeyLength()).thenReturn(22);
        assertThrows(IllegalArgumentException.class, () -> new VsamKey(config));
        when(config.getKeyLength()).thenReturn(23);
        assertDoesNotThrow(() -> new VsamKey(config));
    }


}
