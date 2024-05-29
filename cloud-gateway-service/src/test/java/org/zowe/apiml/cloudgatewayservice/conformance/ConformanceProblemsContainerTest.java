/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.conformance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConformanceProblemsContainerTest {

    ConformanceProblemsContainer container;

    @Nested
    class GivenInputs {

        @BeforeEach
        void setup() {
            container = new ConformanceProblemsContainer("dummy");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 0})
        void whenInserting_thenCorrectSize(int size) {
            ArrayList<String> testList = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                testList.add("testString" + i);
            }
            container.add("test", testList);

            assertEquals(container.size(), testList.size());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 20})
        void whenInsertingSameValue_thenCorrectSize(int size) {
            for (int i = 0; i < size; i++) {
                container.add("test", "value");
            }
            assertEquals(1, container.size());
        }

        @Test
        void whenInserting_thenCanRetrieve() {
            ArrayList<String> testList = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                testList.add("testString" + i);
            }
            container.add("test", testList);
            assertEquals(testList.size(), container.size());
            assertTrue(container.get("test").contains("testString1"));
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 5, 0})
        void whenInsertingToSameKey_thenCorrectSize(int size) {
            for (int i = 0; i < size; i++) {
                container.add("test", "TestString" + i);
            }
            assertEquals(container.size(), size);
        }

        @ParameterizedTest
        @ValueSource(ints = {10, 5, 0})
        void whenInsertingToMultipleKeys_thenCorrectSize(int size) {
            for (int i = 0; i < size; i++) {
                ArrayList<String> testList = new ArrayList<>(Collections.singleton("TestString" + i));
                ArrayList<String> testList2 = new ArrayList<>(Collections.singleton("TestString" + i));
                container.add("test", testList);
                container.add("test", testList);
                container.add("test2", testList2);
            }
            assertEquals(container.size(), 2 * size);
        }

        @Test
        void whenAddingNullValue_thenCorrectSize() {
            container.add("test", (ArrayList<String>) null);
            assertEquals(0, container.size());
        }

        @Test
        void whenAddingNullValue2_thenCorrectSize() {
            container.add("test", (String) null);
            assertEquals(0, container.size());

        }
    }

    @Nested
    class GivenAContainer {

        @BeforeEach
        void setup() {
            container = new ConformanceProblemsContainer("Dummy");
        }

        @Test
        void whenOneItemIn_thenToString() {
            ArrayList<String> result = new ArrayList<>();
            result.add("One");
            result.add("Two");
            container.add("Key", result);
            assertEquals("{\"Key\":[\"One\",\"Two\"]}", container.toString());
        }

        @Test
        void whenTwoItemsIn_thenToString() {
            ArrayList<String> first = new ArrayList<>();
            first.add("One");
            first.add("Two");
            ArrayList<String> second = new ArrayList<>();
            second.add("One");
            second.add("Two");
            container.add("Key", first);
            container.add("Key2", second);
            assertEquals("{\"Key\":[\"One\",\"Two\"],\"Key2\":[\"One\",\"Two\"]}", container.toString());
        }
    }
}
