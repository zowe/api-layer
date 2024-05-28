/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.webfinger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticWebFingerProviderTest {

    static Stream<Arguments> values() {
        return Stream.of(Arguments.of("foobar", 2), Arguments.of("non-exisiting", 0), Arguments.of("0oa6a48mniXAqEMrx5d7", 1));
    }

    @ParameterizedTest
    @MethodSource("values")
    void givenClientId_thenReturnOnlyRelatedRecords(String clientId, int size) throws IOException {
        StaticWebFingerProvider staticWebFingerProvider = new StaticWebFingerProvider();
        ReflectionTestUtils.setField(staticWebFingerProvider, "webfingerDefinition", "../config/local/webfinger.yml");
        WebFingerResponse r = staticWebFingerProvider.getWebFingerConfig(clientId);
        assertEquals(r.getLinks().size(), size);
    }
}
