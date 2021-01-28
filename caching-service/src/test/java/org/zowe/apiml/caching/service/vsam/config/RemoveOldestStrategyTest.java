/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.vsam.VsamFile;
import org.zowe.apiml.caching.service.vsam.VsamFileProducer;
import org.zowe.apiml.caching.service.vsam.VsamRecord;
import org.zowe.apiml.caching.service.vsam.VsamRecordException;
import org.zowe.apiml.zfile.ZFileConstants;
import org.zowe.apiml.zfile.ZFileException;


import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

class RemoveOldestStrategyTest {
    private RemoveOldestStrategy underTest;

    private VsamConfig vsamConfiguration;
    private VsamFileProducer producer;
    private ArgumentCaptor<VsamRecord> recordArgumentCaptor = ArgumentCaptor.forClass(VsamRecord.class);

    private final String VALID_SERVICE_ID = "test-service-id";

    @BeforeEach
    void setUp() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setEvictionStrategy(Strategies.REMOVE_OLDEST.getKey());
        generalConfig.setMaxDataSize(1);
        vsamConfiguration = new VsamConfig(generalConfig);
        vsamConfiguration.setFileName("//'DATASET.NAME'");
        vsamConfiguration.setRecordLength(512);
        vsamConfiguration.setKeyLength(32);
        vsamConfiguration.setEncoding(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);

        producer = mock(VsamFileProducer.class);
        underTest = new RemoveOldestStrategy(vsamConfiguration, producer);
    }

    @Test
    void givenThereIsOneItem_whenEvictIsCalled_thenItIsRemoved() throws ZFileException, VsamRecordException {
        KeyValue record1 = new KeyValue("key-1", "value-1", "1");
        record1.setServiceId(VALID_SERVICE_ID);

        VsamFile returnedFile = mock(VsamFile.class);
        when(producer.newVsamFile(any(), any())).thenReturn(returnedFile);

        VsamRecord fullRecord1 = new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record1);
        when(returnedFile.readBytes(any()))
            .thenReturn(Optional.of(fullRecord1.getBytes()));

        underTest.evict("new-key");
        verify(returnedFile).delete(recordArgumentCaptor.capture());

        VsamRecord deleted = recordArgumentCaptor.getValue();
        assertThat(deleted.getKeyValue().getKey(), is("key-1"));
    }

    @Test
    void givenThereIsMoreItems_whenEvictIsCalled_thenTheOlderOneIsRemoved() throws ZFileException, VsamRecordException {
        KeyValue record1 = new KeyValue("key-1", "value-1", "1");
        record1.setServiceId(VALID_SERVICE_ID);
        KeyValue record2 = new KeyValue("key-2", "value-2", "2");
        record2.setServiceId(VALID_SERVICE_ID);

        VsamFile returnedFile = mock(VsamFile.class);
        when(producer.newVsamFile(any(), any())).thenReturn(returnedFile);

        VsamRecord fullRecord1 = new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record1);
        VsamRecord fullRecord2 = new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record2);
        when(returnedFile.readBytes(any()))
            .thenReturn(Optional.of(fullRecord1.getBytes()))
            .thenReturn(Optional.of(fullRecord2.getBytes()));

        underTest.evict("new-key");
        verify(returnedFile).delete(recordArgumentCaptor.capture());

        VsamRecord deleted = recordArgumentCaptor.getValue();
        assertThat(deleted.getKeyValue().getKey(), is("key-1"));

    }
}
