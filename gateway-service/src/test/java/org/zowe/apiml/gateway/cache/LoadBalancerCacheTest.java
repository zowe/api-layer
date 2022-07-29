/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class LoadBalancerCacheTest {

    static ObjectMapper mapper = new ObjectMapper();
    {
        mapper.registerModule(new JavaTimeModule());
    }
    String keyPrefix = LoadBalancerCache.LOAD_BALANCER_KEY_PREFIX;

    @Nested
    class GivenEmptyCache {
        LoadBalancerCache cache = new LoadBalancerCache(null);

        @Nested
        class WhenItemStoredInCache {
            String user = "USER";
            String service = "DISCOVERABLECLIENT";
            String instance = "discoverable-client:discoverableclient:10012";

            @BeforeEach
            void store() {
                cache.store(user, service, new LoadBalancerCacheRecord(instance));
            }

            @Test
            void itemIsRetrieved() {
                assertThat(cache.retrieve(user, service).getInstanceId(), is(instance));
            }

            @Test
            void afterDeletionItemIsLost() {
                cache.delete(user, service);
                assertThat(cache.retrieve(user, service), is(nullValue()));
            }
        }
    }

    @Nested
    class givenDistributedCache {

        CachingServiceClient cachingServiceClient = mock(CachingServiceClient.class);
        LoadBalancerCache underTest;
        private LoadBalancerCacheRecord record = new LoadBalancerCacheRecord("instanceid");;

        @BeforeEach
        void setUp() {
           underTest = new LoadBalancerCache(cachingServiceClient);
        }

        @Nested
        class Storage {

            @Test
            void storageHappensToLocalAndRemoteCache() throws CachingServiceClientException, JsonProcessingException {
                underTest.store("user", "serviceid", record);
                String serializedRecord = mapper.writeValueAsString(record);
                verify(cachingServiceClient).create(new CachingServiceClient.KeyValue(keyPrefix + "user:serviceid", serializedRecord));
                assertThat(underTest.getLocalCache().containsKey(keyPrefix + "user:serviceid"), is(true));
            }

            @Test
            void storageHappensToRemoteEvenForConflict() throws CachingServiceClientException, JsonProcessingException {
                HttpClientErrorException clientErrorException = HttpClientErrorException.create(HttpStatus.CONFLICT, "", new HttpHeaders(), null, null);
                CachingServiceClientException e = new CachingServiceClientException("oops", clientErrorException);
                doThrow(e).when(cachingServiceClient).create(any());
                String serializedRecord = mapper.writeValueAsString(record);

                underTest.store("user", "serviceid", record);
                verify(cachingServiceClient).update(new CachingServiceClient.KeyValue(keyPrefix + "user:serviceid", serializedRecord));
            }

            @Test
            void storageFailsToRemoteCacheAndStoresLocal() throws CachingServiceClientException {
                doThrow(CachingServiceClientException.class).when(cachingServiceClient).create(any());
                underTest.store("user", "serviceid", record);
                assertThat(underTest.getLocalCache().containsKey(keyPrefix + "user:serviceid"), is(true));
            }
        }

        @Nested
        class Retrieval {

            @Test
            void retrievalFromRemoteHasPriority() throws CachingServiceClientException, JsonProcessingException {
                underTest.getLocalCache().put(keyPrefix + "user:serviceid", record);
                doThrow(CachingServiceClientException.class).when(cachingServiceClient).read(any());
                LoadBalancerCacheRecord retrievedRecord = underTest.retrieve("user", "serviceid");
                assertThat(retrievedRecord.getInstanceId(), is("instanceid"));

                LoadBalancerCacheRecord record = new LoadBalancerCacheRecord("Batman");
                String serialzedRecord = mapper.writeValueAsString(record);
                doReturn(new CachingServiceClient.KeyValue(keyPrefix + "user:serviceid", serialzedRecord)).when(cachingServiceClient).read(keyPrefix + "user:serviceid");

                retrievedRecord = underTest.retrieve("user", "serviceid");
                assertThat(retrievedRecord.getInstanceId(), is("Batman"));
            }

        }

        @Nested
        class Deletion {
            @Test
            void deleteRemovesAllEntriesLocalAndRemote() throws CachingServiceClientException {
                underTest.getLocalCache().put(keyPrefix + "user:serviceid", record);
                underTest.delete("user", "serviceid");
                verify(cachingServiceClient).delete(keyPrefix + "user:serviceid");
                assertThat(underTest.retrieve("user", "serviceid"), is(nullValue()));

            }
        }
    }
}
