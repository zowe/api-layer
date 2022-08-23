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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.inmemory.config.InMemoryConfig;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class InMemoryStorageTest {
    private InMemoryStorage underTest;
    private InMemoryConfig config;

    private Map<String, Map<String, KeyValue>> testingStorage;
    private final String serviceId = "acme";

    @BeforeEach
    void setUp() {
        testingStorage = new HashMap<>();
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setEvictionStrategy(Strategies.REJECT.getKey());
        config = new InMemoryConfig(generalConfig);
        config.getGeneralConfig().setMaxDataSize(10);
        underTest = new InMemoryStorage(config, testingStorage, ApimlLogger.empty());
    }

    @Test
    void givenDefaultStorageConstructor_whenStorageConstructed_thenCanUseStorage() {
        underTest = new InMemoryStorage(config, mock(MessageService.class));
        underTest.create(serviceId, new KeyValue("key", "value"));

        KeyValue result = underTest.read(serviceId, "key");
        assertThat(result.getKey(), is("key"));
        assertThat(result.getValue(), is("value"));
    }

    @Nested
    class WhenValueIsStored {
        @Test
        void givenThereIsNoValueForService_thenItIsStored() {
            underTest.create(serviceId, new KeyValue("username", "ValidName"));

            KeyValue result = testingStorage.get(serviceId).get("username");
            assertThat(result.getKey(), is("username"));
            assertThat(result.getValue(), is("ValidName"));
        }

        // Given the strategy is reject
        @Test
        void givenTheStorageIsFullAndStrategyIsReject_thenTheInsufficientStorageExceptionIsRaised() {
            GeneralConfig generalConfig = new GeneralConfig();
            generalConfig.setEvictionStrategy(Strategies.REJECT.getKey());
            config = new InMemoryConfig(generalConfig);
            config.getGeneralConfig().setMaxDataSize(1);

            underTest = new InMemoryStorage(config, testingStorage, ApimlLogger.empty());
            underTest.create("customService", new KeyValue("key", "willFit"));
            KeyValue wontFit = new KeyValue("key", "wontFit");
            assertThrows(StorageException.class, () -> {
                underTest.create(serviceId, wontFit);
            });
        }

        @Test
        void givenTheStorageIsFullAndStrategyIsRemoveOldest_thenTheOldestErrorIsEvicted() {
            GeneralConfig generalConfig = new GeneralConfig();
            generalConfig.setEvictionStrategy(Strategies.REMOVE_OLDEST.getKey());
            config = new InMemoryConfig(generalConfig);
            config.getGeneralConfig().setMaxDataSize(1);

            String oldestKey = "oldestKey";
            underTest = new InMemoryStorage(config, testingStorage, ApimlLogger.empty());

            KeyValue keyValue1 = new KeyValue(oldestKey, "willFit", "1");
            keyValue1.setServiceId(serviceId);

            KeyValue keyValue2 = new KeyValue("key1", "willFit", "2");
            keyValue2.setServiceId(serviceId);

            underTest.create(serviceId, keyValue1);
            underTest.create(serviceId, keyValue2);

            assertThrows(StorageException.class, () -> {
                underTest.read(serviceId, oldestKey);
            });
        }
    }


    @Nested
    class WhenValueIsUpdated {
        @Test
        void givenThereIsValueForService_thenItIsReplaced() {
            Map<String, KeyValue> serviceStorage = new HashMap<>();
            testingStorage.put(serviceId, serviceStorage);
            serviceStorage.put("username", new KeyValue("username", "Name 1"));
            underTest.update(serviceId, new KeyValue("username", "ValidName"));

            KeyValue result = testingStorage.get(serviceId).get("username");
            assertThat(result.getKey(), is("username"));
            assertThat(result.getValue(), is("ValidName"));
        }

        @Test
        void givenThereIsNoServiceCache_thenNullIsReturned() {
            KeyValue keyValue = new KeyValue("username", "Name 1");
            assertThrows(StorageException.class, () -> {
                underTest.update(serviceId, keyValue);
            });
        }

        @Test
        void givenThereIsNoKey_thenNullIsReturned() {
            testingStorage.put(serviceId, new HashMap<>());
            KeyValue keyValue = new KeyValue("bad key", "Name 1");
            assertThrows(StorageException.class, () -> {
                underTest.update(serviceId, keyValue);
            });
        }
    }

    @Nested
    class WhenValueIsRetrieved {
        @Test
        void givenValueWasAlreadyAddedToTheStorage_thenItWillBeReturned() {
            Map<String, KeyValue> serviceStorage = new HashMap<>();
            testingStorage.put(serviceId, serviceStorage);
            serviceStorage.put("username", new KeyValue("username", "Name 1"));

            KeyValue result = underTest.read(serviceId, "username");
            assertThat(result.getKey(), is("username"));
            assertThat(result.getValue(), is("Name 1"));
        }

        @Test
        void givenNoValueWasStoredForTheService_thenNullWillBeReturned() {
            assertThrows(StorageException.class, () -> {
                underTest.read(serviceId, "username");
            });
        }
    }

    @Nested
    class WhenDeletionRequested {
        @Test
        void givenKeyDoesntExist_thenNullIsReturned() {
            testingStorage.put(serviceId, new HashMap<>());
            assertThrows(StorageException.class, () -> {
                underTest.delete(serviceId, "nonexistent");
            });
        }

        @Test
        void givenServiceStorageDoesntExist_thenNullIsReturned() {
            assertThrows(StorageException.class, () -> {
                underTest.delete(serviceId, "nonexistent");
            });
        }

        @Test
        void givenKeyExists_thenKeyValueIsReturnedAndKeyIsRemoved() {
            Map<String, KeyValue> serviceStorage = new HashMap<>();
            testingStorage.put(serviceId, serviceStorage);
            serviceStorage.put("username", new KeyValue("username", "Name 1"));

            underTest.delete(serviceId, "username");
            assertThat(serviceStorage.containsKey("username"), is(false));
        }
    }

    @Nested
    class WhenLoadingAllForService {
        @Test
        void givenServiceHasStoredValues_thenAllAreReturned() {
            Map<String, KeyValue> serviceStorage = new HashMap<>();
            testingStorage.put(serviceId, serviceStorage);
            serviceStorage.put("username", new KeyValue("username", "Name 1"));

            Map<String, KeyValue> result = underTest.readForService(serviceId);
            assertThat(result.containsKey("username"), is(true));
        }
    }

    @Nested
    class WhenDeletingAllForService {
        @Test
        void givenServiceHasStoredValues_thenNoneRemains() {
            Map<String, KeyValue> serviceStorage = new HashMap<>();
            testingStorage.put(serviceId, serviceStorage);
            serviceStorage.put("username", new KeyValue("username", "Name 1"));

            underTest.deleteForService(serviceId);

            assertThat(testingStorage.containsKey("username"), is(false));
        }
    }

    @Nested
    class WhenTryingToStoreToken {
        @Test
        void thenThrowException() {
            KeyValue keyValue = new KeyValue("key", "value");
            assertThrows(StorageException.class, () -> {
                underTest.storeMapItem(serviceId, "mapKey", keyValue);
            });
        }
    }

    @Nested
    class WhenTryingToGetTokens {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.getAllMapItems(serviceId, "key");
            });
        }
    }

    @Nested
    class WhenTryingToGetAllMaps {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.getAllMaps(serviceId);
            });
        }
    }

    @Nested
    class WhenTryingToDeleteTokens {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.removeNonRelevantTokens(serviceId, "key");
            });
        }
    }

    @Nested
    class WhenTryingToDeleteRules {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.removeNonRelevantRules(serviceId, "key");
            });
        }
    }
}
