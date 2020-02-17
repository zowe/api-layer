/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

public class MapUtilsTest {

    private MapUtils mapUtils = new MapUtils();

    @Test
    public void givenMap_whenFlattenedWithRootKey_shouldReturnMapKeysWithRootKey() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", null);
        Map<String, String> resultMap = mapUtils.flattenMap("apiml", testMap);
        assertThat(resultMap, hasEntry("apiml.key", ""));
    }

    @Test
    public void givenMapWithNullValue_whenFlattened_shouldReturnValueEmptyString() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", null);
        Map<String, String> resultMap = mapUtils.flattenMap(null, testMap);
        assertThat(resultMap, hasEntry("key", ""));
    }

    @Test
    public void givenMapWithPrimitiveValues_whenFlattened_shouldReturnCorrectValues() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key1", true);
        testMap.put("key2", 23);
        testMap.put("key3", 23.0d);
        testMap.put("key4", 23.0f);
        Map<String, String> resultMap = mapUtils.flattenMap(null, testMap);
        assertThat(resultMap, hasEntry("key1", "true"));
        assertThat(resultMap, hasEntry("key2", "23"));
        assertThat(resultMap, hasEntry("key3", "23.0"));
        assertThat(resultMap, hasEntry("key4", "23.0"));
    }

    @Test
    public void givenMapWithNestedMap_whenFlattened_shouldReturnFlattened() {
        Map<String, Object> nestedLvl2 = new HashMap<>();
        nestedLvl2.put("keyzzz", "valuezzz");

        Map<String, Object> nested = new HashMap<>();
        nested.put("key1", "value1");
        nested.put("key2", "value2");
        nested.put("key3", nestedLvl2);

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("masterKey", nested);

        Map<String, String> resultMap = mapUtils.flattenMap(null, testMap);
        assertThat(resultMap, hasEntry("masterKey.key1", "value1"));
        assertThat(resultMap, hasEntry("masterKey.key2", "value2"));
        assertThat(resultMap, hasEntry("masterKey.key3.keyzzz", "valuezzz"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenMapWithNestedList_whenFlattened_shouldReturnException() {
        List<Object> nested = new ArrayList<>();
        nested.add("value1");
        nested.add("value2");

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("masterKey", nested);

        mapUtils.flattenMap(null, testMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenMapWithNestedArray_whenFlattened_shouldReturnException() {
        String[] nested = {"value1", "value2"};

        Map<String, Object> testMap = new HashMap<>();
        testMap.put("masterKey", nested);

        mapUtils.flattenMap(null, testMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenMapWithAnythingElseThanExpected_whenFlattened_shouldReturnException() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key1", new BigDecimal(0));
        mapUtils.flattenMap(null, testMap);
    }

}
