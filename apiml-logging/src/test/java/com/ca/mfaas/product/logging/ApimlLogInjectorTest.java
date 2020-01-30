/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.logging;

import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringRunner.class)
@Import(ApimlLogInjectorTest.TestConfig.class)
public class ApimlLogInjectorTest {

    @Autowired
    private TestComponent testComponent;

    @Test
    public void testInject() {
        assertNotNull(testComponent.apimlLogger);
        assertNull(testComponent.apimlLogger2);
    }


    private static class TestComponent {

        @InjectApimlLogger
        private ApimlLogger apimlLogger;


        private ApimlLogger apimlLogger2;

    }


    @SpringBootConfiguration
    public static class TestConfig {

        @Bean
        public ApimlLogInjector apimlLogInjector() {
            return new ApimlLogInjector();
        }

        @Bean
        public TestComponent testComponent() {
            return new ApimlLogInjectorTest.TestComponent();
        }

    }
}
