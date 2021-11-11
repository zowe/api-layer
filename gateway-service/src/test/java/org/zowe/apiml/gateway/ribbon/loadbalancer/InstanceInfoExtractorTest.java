/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstanceInfoExtractorTest {

    @Nested
    class givenListOfNetflixDiscoveryEnabledServers {


        private List<Server> serverList = new ArrayList<>();

        @BeforeEach
        void setUp() {
            serverList.add(getDiscoveryEnabledServer("instance1"));
            serverList.add(getDiscoveryEnabledServer("instance2"));
        }

        @Test
        void extractsRandomInstanceInfo() {
            InstanceInfoExtractor underTest = new InstanceInfoExtractor(serverList);
            Optional<InstanceInfo> info = underTest.getInstanceInfo();
            assertThat(info, is(notNullValue()));
            assertThat(info.isPresent(), is(true));
        }

        @Test
        void extractEmptyFromEmptyList() {
            InstanceInfoExtractor underTest = new InstanceInfoExtractor(new ArrayList<Server>());
            Optional<InstanceInfo> info = underTest.getInstanceInfo();
            assertThat(info.isPresent(), is(false));
        }
    }

    @Nested
    class givenListOfNetflixIncompatibleServers {


        private List<Server> serverList = new ArrayList<>();

        @BeforeEach
        void setUp() {
            serverList.add(new Server("host", 443));
        }

        @Test
        void failFast() {
            assertThrows(IllegalArgumentException.class, () -> new InstanceInfoExtractor(serverList));
        }
    }

    private DiscoveryEnabledServer getDiscoveryEnabledServer(String instanceId) {
        InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();
        builder.setAppName("app");
        builder.setHostName("mordor");
        builder.setInstanceId(instanceId);
        builder.setSecurePort(443);
        DiscoveryEnabledServer s1 = new DiscoveryEnabledServer(builder.build(), true);
        return s1;
    }

}
