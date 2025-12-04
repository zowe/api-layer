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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Nested
    class OnStart {

        @Test
        void whenValues_thenSucess() {
            propertyContainsCondition.key = "key";
            propertyContainsCondition.value = "value";

            propertyContainsCondition.start();

            verify(propertyContainsCondition, never()).addError(anyString());
        }

        @Test
        void whenMissingKey_thenError() {
            doNothing().when(propertyContainsCondition).addError(any());

            propertyContainsCondition.key = null;
            propertyContainsCondition.value = "value";

            propertyContainsCondition.start();

            verify(propertyContainsCondition, times(1)).addError(argThat(message -> message.contains("'key'")));
        }

        @Test
        void whenMissingValue_thenError() {
            doNothing().when(propertyContainsCondition).addError(any());

            propertyContainsCondition.key = "key";
            propertyContainsCondition.value = null;

            propertyContainsCondition.start();

            verify(propertyContainsCondition, times(1)).addError(argThat(message -> message.contains("'value'")));
        }

    }

}
