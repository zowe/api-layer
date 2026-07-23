/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.functional;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;

/**
 * Starts the full Discovery Service context and registers a real service through the Eureka instance registry.
 * Verifies that the missing-APPLID warning (ZWEAD707) for the httpBasicPassTicket scheme is emitted exactly at
 * registration - the moment we want it reported - and not on any later route/heartbeat processing.
 */
class MissingApplidRegistrationTest extends DiscoveryFunctionalTest {

    @Autowired
    private ApimlInstanceRegistry registry;

    private final Logger registryLogger = (Logger) LoggerFactory.getLogger(ApimlInstanceRegistry.class);
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void attachLogAppender() {
        logAppender.list.clear();
        logAppender.start();
        registryLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        registryLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    private InstanceInfo passTicketInstance(String serviceId, String applid) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SCHEME, AuthenticationScheme.HTTP_BASIC_PASSTICKET.getScheme());
        if (applid != null) {
            metadata.put(AUTHENTICATION_APPLID, applid);
        }
        return InstanceInfo.Builder.newBuilder()
            .setInstanceId("localhost:" + serviceId + ":10010")
            .setAppName(serviceId)
            .setIPAddr("127.0.0.1")
            .setSecurePort(10010)
            .setHostName("localhost")
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setMetadata(metadata)
            .build();
    }

    @Test
    void givenServiceWithPassTicketAndNoApplid_whenRegistered_thenWarningIsLogged() {
        registry.register(passTicketInstance("SERVICEUSINGPASSTICKET", null), false);

        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .anyMatch(message -> message.contains("ZWEAD707W")
                && message.toLowerCase().contains("serviceusingpassticket")
                && message.contains("without a configured APPLID"));
    }

    @Test
    void givenServiceWithPassTicketAndApplid_whenRegistered_thenNoWarning() {
        registry.register(passTicketInstance("SERVICEWITHAPPLID", "IZUDFLT"), false);

        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .noneMatch(message -> message.contains("ZWEAD707"));
    }

}
