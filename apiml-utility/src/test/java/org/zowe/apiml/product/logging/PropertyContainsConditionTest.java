/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.logging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PropertyContainsConditionTest {

    @Spy
    private PropertyContainsCondition propertyContainsCondition;

    @Nested
    class OnEvaluate {

        @ParameterizedTest
        @CsvSource(value = {
            "key,abc,b,true",
            "key,abc,a,true",
            "key,abc,c,true",
            "key,abc,d,false",
            "key,abc,B,false",
            "null,abc,b,false",
            "key,null,b,false"
        }, delimiter = ',', nullValues = { "null" })
        void expectResult(String key, String returnedValue, String searchedForValue, boolean expectedResult) {
            propertyContainsCondition.setKey(key);
            propertyContainsCondition.setValue(searchedForValue);
            lenient().doReturn(returnedValue).when(propertyContainsCondition).p(key);
            assertEquals(expectedResult, propertyContainsCondition.evaluate(), "Expected " + returnedValue + " to " + (expectedResult ? " contain " : " not contain ") + searchedForValue);
        }

    }

}
