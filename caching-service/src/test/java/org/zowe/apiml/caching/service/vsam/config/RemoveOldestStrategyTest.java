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
import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.vsam.VsamFile;
import org.zowe.apiml.caching.service.vsam.VsamFileProducer;
import org.zowe.apiml.zfile.ZFile;
import org.zowe.apiml.zfile.ZFileConstants;
import org.zowe.apiml.zfile.ZFileException;


import static org.mockito.Mockito.*;

class RemoveOldestStrategyTest {

    private VsamConfig vsamConfiguration;
    private VsamFileProducer producer;
    private RemoveOldestStrategy underTest;

    @BeforeEach
    void setUp() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setEvictionStrategy(Strategies.REMOVE_OLDEST.getKey());
        generalConfig.setMaxDataSize(1);
        producer = mock(VsamFileProducer.class);
        vsamConfiguration = new VsamConfig(generalConfig);
        vsamConfiguration.setFileName("//'DATASET.NAME'");
        vsamConfiguration.setRecordLength(512);
        vsamConfiguration.setKeyLength(32);
        vsamConfiguration.setEncoding(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        underTest = new RemoveOldestStrategy(vsamConfiguration, producer);

    }

    @Test
    void remove_oldest() throws ZFileException {

        KeyValue record = new KeyValue("key-1", "value-1", "test-service-id", "1");
        KeyValue record2 = new KeyValue("key-2", "value-2", "test-service-id", "1");
        VsamFile returnedFile = mock(VsamFile.class);
        when(producer.newVsamFile(any(), any())).thenReturn(returnedFile);
        ZFile mockedZfile = mock(ZFile.class);
        when(returnedFile.getZfile()).thenReturn(mockedZfile);
        when(returnedFile.getZfile().locate(any(), eq(ZFileConstants.LOCATE_KEY_EQ))).thenReturn(true);
        underTest.evict(record.getKey());
        verify(returnedFile, times(2)).getZfile();

    }
}
