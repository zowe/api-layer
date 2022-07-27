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
import org.zowe.apiml.caching.service.EvictionStrategy;
import org.zowe.apiml.caching.service.RejectStrategy;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.core.MessageService;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

class EvictionStrategyProducerTest {
    private EvictionStrategyProducer underTest;
    private GeneralConfig generalConfig;
    private VsamConfig vsamConfig;
    private MessageService messageService;

    private VsamFile vsamFile;

    @BeforeEach
    void setUp() {
        generalConfig = new GeneralConfig();
        vsamConfig = new VsamConfig(generalConfig);
        messageService = mock(MessageService.class);

        vsamFile = mock(VsamFile.class);
    }

    @Nested
    class WhenGetStrategy {
        @Test
        void givenUseOldStrategy_thenReturnOldestStrategy() {
            generalConfig.setEvictionStrategy(Strategies.REMOVE_OLDEST.getKey());

            underTest = new EvictionStrategyProducer(generalConfig, vsamConfig, messageService);
            EvictionStrategy result = underTest.evictionStrategy(vsamFile);
            assertThat(result, instanceOf(RemoveOldestStrategy.class));
        }

        @Test
        void givenUseRejectStrategy_thenReturnRejectStrategy() {
            generalConfig.setEvictionStrategy(Strategies.REJECT.getKey());

            underTest = new EvictionStrategyProducer(generalConfig, vsamConfig, messageService);
            EvictionStrategy result = underTest.evictionStrategy(vsamFile);
            assertThat(result, instanceOf(RejectStrategy.class));
        }

        @Test
        void givenExistingRejectStrategy_thenReturnExistingStrategy() {
            generalConfig.setEvictionStrategy(Strategies.REJECT.getKey());
            underTest = new EvictionStrategyProducer(generalConfig, vsamConfig, messageService);

            EvictionStrategy first = underTest.evictionStrategy(vsamFile);
            EvictionStrategy second = underTest.evictionStrategy(vsamFile);
            assertThat(second, is(first));

        }
    }
}
