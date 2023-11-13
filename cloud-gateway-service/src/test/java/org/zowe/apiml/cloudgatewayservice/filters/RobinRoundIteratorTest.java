/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.filters;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RobinRoundIteratorTest {

    private List<Integer> fetch(Iterator<Integer> iterator) {
        List<Integer> output = new LinkedList<>();
        while (iterator.hasNext()) {
            output.add(iterator.next());
        }
        return output;
    }

    @Nested
    class GivenSameCollection {

        @Test
        void givenLongerCollections_whenIterateThem_thenCycleOrder() {
            RobinRoundIterator<Integer> rri = new RobinRoundIterator<>();

            Collection<Integer> input = Arrays.asList(1, 2, 3);
            assertIterableEquals(Arrays.asList(1, 2, 3), fetch(rri.getIterator(input)));
            assertIterableEquals(Arrays.asList(2, 3, 1), fetch(rri.getIterator(input)));
            assertIterableEquals(Arrays.asList(3, 1, 2), fetch(rri.getIterator(input)));
            assertIterableEquals(Arrays.asList(1, 2, 3), fetch(rri.getIterator(input)));
        }

        @Test
        void givenEmptyCollection_whenTransform_thenReturnEmptyCollection() {
            RobinRoundIterator<Integer> rri = new RobinRoundIterator<>();

            assertIterableEquals(Collections.emptyList(), fetch(rri.getIterator(Collections.emptyList())));
        }

        @Test
        void givenSingletonList_whenTransform_theOrderIsTheSame() {
            RobinRoundIterator<Integer> rri = new RobinRoundIterator<>();

            assertIterableEquals(Collections.singleton(1), fetch(rri.getIterator(Collections.singleton(1))));
            assertIterableEquals(Collections.singleton(1), fetch(rri.getIterator(Collections.singleton(1))));
        }

    }

    @Nested
    class MutableCollection {

        @Test
        void givenListWithDifferentLength_whenTransform_theOffsetIsStable() {
            RobinRoundIterator<Integer> rri = new RobinRoundIterator<>();

            assertIterableEquals(Arrays.asList(1, 2, 3), fetch(rri.getIterator(Arrays.asList(1, 2, 3))));
            assertIterableEquals(Arrays.asList(2, 1), fetch(rri.getIterator(Arrays.asList(1, 2))));
            assertIterableEquals(Arrays.asList(3, 1, 2), fetch(rri.getIterator(Arrays.asList(1, 2, 3))));
            assertIterableEquals(Arrays.asList(4, 1, 2, 3), fetch(rri.getIterator(Arrays.asList(1, 2, 3, 4))));
            assertIterableEquals(Arrays.asList(2, 3, 1), fetch(rri.getIterator(Arrays.asList(1, 2, 3))));
        }

    }

    @Nested
    class EdgeCases {

        @Test
        void givenACollection_whenIterateOver_thenThrowAnException() {
            RobinRoundIterator<Integer> rri = new RobinRoundIterator<>();
            Iterator<Integer> i = rri.getIterator(Collections.emptyList());
            assertFalse(i.hasNext());
            assertThrows(NoSuchElementException.class, i::next);
        }

    }

}