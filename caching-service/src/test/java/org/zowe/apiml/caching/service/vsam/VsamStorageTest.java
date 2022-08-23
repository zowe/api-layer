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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.RejectStrategy;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.log.ApimlLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VsamStorageTest {
    private VsamStorage underTest;
    private final String VALID_SERVICE_ID = "test-service-id";
    private final ApimlLogger apimlLogger = ApimlLogger.empty();

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

        EvictionStrategyProducer evictionStrategyProducer = mock(EvictionStrategyProducer.class);
        when(evictionStrategyProducer.evictionStrategy(any())).thenReturn(new RejectStrategy(apimlLogger));
        underTest = new VsamStorage(vsamConfiguration, initializer, producer, apimlLogger, evictionStrategyProducer);
    }

    @Test
    void givenNoInvalidFilename_whenCreateVsamStorage_thenThrowException() {
        VsamInitializer initializer = mock(VsamInitializer.class);
        EvictionStrategyProducer evictionStrategyProducer = mock(EvictionStrategyProducer.class);
        when(evictionStrategyProducer.evictionStrategy(any())).thenReturn(new RejectStrategy(apimlLogger));
        VsamConfig vsamConfig = new VsamConfig(new GeneralConfig());

        vsamConfig.setFileName(null);
        assertThrows(IllegalArgumentException.class, () -> new VsamStorage(vsamConfig, initializer, apimlLogger, evictionStrategyProducer));

        vsamConfig.setFileName("");
        assertThrows(IllegalArgumentException.class, () -> new VsamStorage(vsamConfig, initializer, apimlLogger, evictionStrategyProducer));
    }

    @Nested
    class WhenItemIsCreated {
        @Test
        void givenValidServiceIdKeyValue_thenItIsProperlyReturned() {
            KeyValue record = new KeyValue("key-1", "value-1", "1");
            record.setServiceId(VALID_SERVICE_ID);
            VsamFile returnedFile = mock(VsamFile.class);
            when(returnedFile.countAllRecords()).thenReturn(60);
            when(returnedFile.create(any())).thenReturn(
                Optional.of(new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record))
            );
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            KeyValue result = underTest.create(VALID_SERVICE_ID, record);
            assertThat(result, is(record));
        }

        @Test
        void givenTheKeyAlreadyExists_thenExceptionIsThrown() {
            KeyValue record = new KeyValue("key-1", "value-1", "1");
            record.setServiceId(VALID_SERVICE_ID);
            VsamFile returnedFile = mock(VsamFile.class);
            when(returnedFile.countAllRecords()).thenReturn(60);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            assertThrows(StorageException.class, () -> {
                underTest.create(VALID_SERVICE_ID, record);
            });
        }

        @Test
        void givenTheSizeWasExceeded_thenTheExceptionIsThrownInReject() {
            KeyValue record = new KeyValue("key-1", "value-1", "1");
            record.setServiceId(VALID_SERVICE_ID);

            VsamFile returnedFile = mock(VsamFile.class);
            when(returnedFile.countAllRecords()).thenReturn(200);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            assertThrows(StorageException.class, () -> {
                underTest.create(VALID_SERVICE_ID, record);
            });
        }
    }

    @Nested
    class WhenItemIsRead {
        @Test
        void givenKeyIsInCache_thenItIsReturned() {
            KeyValue record = new KeyValue("key-1", "value-1", "1");
            record.setServiceId(VALID_SERVICE_ID);
            VsamFile returnedFile = mock(VsamFile.class);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            when(returnedFile.read(any())).thenReturn(
                Optional.of(new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record))
            );

            KeyValue result = underTest.read(VALID_SERVICE_ID, "existing-key");
            assertThat(result, is(record));
        }

        @Test
        void givenKeyIsntInCache_thenExceptionIsThrown() {
            VsamFile returnedFile = mock(VsamFile.class);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            when(returnedFile.read(any())).thenReturn(
                Optional.empty()
            );
            assertThrows(StorageException.class, () -> {
                underTest.read(VALID_SERVICE_ID, "non-existing-key");
            });
        }
    }

    @Nested
    class WhenItemIsUpdated {
        @Test
        void givenKeyIsInCache_thenItIsUpdated() {
            KeyValue record = new KeyValue("key-1", "value-1", "1");
            record.setServiceId(VALID_SERVICE_ID);
            VsamFile returnedFile = mock(VsamFile.class);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            when(returnedFile.update(any())).thenReturn(
                Optional.of(new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record))
            );

            KeyValue result = underTest.update(VALID_SERVICE_ID, record);
            assertThat(result, is(record));
        }

        @Test
        void givenKeyIsntInCache_thenExceptionIsThrown() {
            KeyValue record = new KeyValue("key-1", "value-1", "1");
            record.setServiceId(VALID_SERVICE_ID);

            VsamFile returnedFile = mock(VsamFile.class);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            when(returnedFile.read(any())).thenReturn(
                Optional.empty()
            );
            assertThrows(StorageException.class, () -> {
                underTest.update(VALID_SERVICE_ID, record);
            });
        }
    }

    @Nested
    class WhenItemIsDeleted {
        @Test
        void givenKeyIsInCache_thenItIsDeleted() {
            KeyValue record = new KeyValue("key-1", "value-1", "1");
            record.setServiceId(VALID_SERVICE_ID);

            VsamFile returnedFile = mock(VsamFile.class);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            when(returnedFile.delete(any())).thenReturn(
                Optional.of(new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record))
            );

            KeyValue result = underTest.delete(VALID_SERVICE_ID, "key-1");
            assertThat(result, is(record));
        }

        @Test
        void givenKeyIsntInCache_thenExceptionIsThrown() {
            VsamFile returnedFile = mock(VsamFile.class);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            when(returnedFile.read(any())).thenReturn(
                Optional.empty()
            );
            assertThrows(StorageException.class, () -> {
                underTest.delete(VALID_SERVICE_ID, "key-1");
            });
        }
    }

    @Nested
    class WhenRequestAllForService {
        @Test
        void givenValueForService_thenAllAreReturned() {
            KeyValue record1 = new KeyValue("key-1", "value-1", "1");
            record1.setServiceId(VALID_SERVICE_ID);

            KeyValue record2 = new KeyValue("key-2", "value-2", "2");
            record2.setServiceId(VALID_SERVICE_ID);

            VsamFile returnedFile = mock(VsamFile.class);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            List<VsamRecord> records = new ArrayList<>();
            records.add(new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record1));
            records.add(new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, record2));

            when(returnedFile.readForService(VALID_SERVICE_ID)).thenReturn(records);

            Map<String, KeyValue> tests = underTest.readForService(VALID_SERVICE_ID);
            assertThat(tests.get("key-1"), is(record1));
            assertThat(tests.get("key-2"), is(record2));
        }
    }

    @Nested
    class WhenDeleteAllForService {
        @Test
        void givenValidVsamFileIsCreated_thenAllAreRemoved() {
            VsamFile returnedFile = mock(VsamFile.class);
            when(producer.newVsamFile(any(), any(), any())).thenReturn(returnedFile);

            underTest.deleteForService(VALID_SERVICE_ID);

            verify(returnedFile).deleteForService(VALID_SERVICE_ID);
        }
    }

    @Nested
    class WhenTryingToStoreToken {
        @Test
        void thenThrowException() {
            KeyValue keyValue = new KeyValue("key", "value");
            assertThrows(StorageException.class, () -> {
                underTest.storeMapItem("serviceId", "mapkey", keyValue);
            });
        }
    }

    @Nested
    class WhenTryingToGetTokens {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.getAllMapItems("serviceId", "key");
            });
        }
    }

    @Nested
    class WhenTryingToGetAllMaps {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.getAllMaps("serviceId");
            });
        }
    }

    @Nested
    class WhenTryingToDeleteTokens {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.removeNonRelevantTokens("serviceId", "key");
            });
        }
    }

    @Nested
    class WhenTryingToDeleteRules {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.removeNonRelevantRules("serviceId", "key");
            });
        }
    }
}
