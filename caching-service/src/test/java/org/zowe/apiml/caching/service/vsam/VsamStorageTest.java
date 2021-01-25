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
import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VsamStorageTest {
    private VsamStorage underTest;

    private VsamFileProducer producer;
    private VsamConfig vsamConfiguration;

    @BeforeEach
    void setUp() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setEvictionStrategy(Strategies.REJECT.getKey());
        generalConfig.setMaxDataSize(100);
        vsamConfiguration = new VsamConfig(generalConfig);
        vsamConfiguration.setFileName("test-file-name");
        vsamConfiguration.setRecordLength(512);
        vsamConfiguration.setKeyLength(64);

        VsamInitializer initializer = mock(VsamInitializer.class);
        producer = mock(VsamFileProducer.class);

        underTest = new VsamStorage(vsamConfiguration, initializer, producer);
    }

    @Test
    void givenValidServiceIdKeyValue_whenItemIsCreated_thenItIsProperlyReturned() {
        KeyValue record = new KeyValue("key-1", "value-1", "test-service-id", "1");

        VsamFile returnedFile = mock(VsamFile.class);
        when(returnedFile.countAllRecords()).thenReturn(60);
        when(returnedFile.create(any())).thenReturn(
            Optional.of(new VsamRecord(vsamConfiguration, "test-service-id", record))
        );
        when(producer.newVsamFile(any(), any())).thenReturn(returnedFile);

        KeyValue result = underTest.create("test-service-id", record);
        assertThat(result, is(record));
    }
}
