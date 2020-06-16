/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.metadata.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class MetadataProcessorTest {

    private MetadataProcessor metadataProcessor;

    @BeforeEach
    void setMetadataProcessor() {
        metadataProcessor = new MetadataProcessor(mock(EurekaApplications.class));
    }

    @Test
    void givenMetadata_whenProcess_thenSetSystemProperties() {
        metadataProcessor.process(
            prepareApplications(createValidMetadata())
        );

        assertThatValuesAreSet("5000", "5000", "5000");
    }

    @Test
    void givenEmptyMetadata_whenProcess_thenDoNotSetSystemProperties() {
        metadataProcessor.process(
            prepareApplications(null)
        );

        assertThatValuesAreSet(null, null, null);
    }

    @Test
    void givenNonNumericMetadata_whenProcess_thenDoNotSetSystemProperties() {
        metadataProcessor.process(
            prepareApplications(createNonNumericMetadata())
        );

        assertThatValuesAreSet(null, "5000", "5000");
    }

    @Test
    void givenMockedEvent_whenOnEvent_thenCallProcess() {
        EurekaEvent event = mock(EurekaEvent.class);
        MetadataProcessor metadataProcessor = new MetadataProcessor(mock(EurekaApplications.class));
        MetadataProcessor spyProcessor = spy(metadataProcessor);
        spyProcessor.onEvent(event);

        verify(spyProcessor, times(1)).process(any());
    }

    private void assertThatValuesAreSet(String connectTimeout, String readTimeout, String connectionManagerTimeout) {
        assertThat(System.getProperty("service.ribbon.ConnectTimeout"), is(connectTimeout));
        assertThat(System.getProperty("service.ribbon.ReadTimeout"), is(readTimeout));
        assertThat(System.getProperty("service.ribbon.ConnectionManagerTimeout"), is(connectionManagerTimeout));
    }

    private List<Application> prepareApplications(Map<String, String> metadata) {
        List<Application> applications = new ArrayList<>();
        Application application = mock(Application.class);
        applications.add(application);
        Mockito.when(application.getInstances()).thenReturn(Collections.singletonList(getStandardInstance(metadata)));
        return applications;
    }

    private InstanceInfo getStandardInstance(Map<String, String> metadata) {
        return InstanceInfo.Builder.newBuilder()
            .setAppName("service")
            .setHostName("localhost")
            .setVIPAddress("service")
            .setMetadata(metadata)
            .build();
    }

    private Map<String, String> createValidMetadata() {
        return createMetadata("5000");
    }

    private Map<String, String> createNonNumericMetadata() {
        return createMetadata("hello");
    }

    private Map<String, String> createMetadata(String connectTimeout) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.connectTimeout", connectTimeout);
        metadata.put("apiml.readTimeout", "5000");
        metadata.put("apiml.connectionManagerTimeout", "5000");
        return metadata;
    }
}
