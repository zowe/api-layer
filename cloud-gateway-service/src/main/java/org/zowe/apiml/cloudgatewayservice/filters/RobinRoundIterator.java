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

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public class RobinRoundIterator<T> {

    private final AtomicInteger lastIndex = new AtomicInteger(-1);

    public Iterator<T> getIterator(Collection<T> input) {
        int offset = lastIndex.updateAndGet(prev -> input.isEmpty() ? 0 : (prev + 1) % input.size());

        return new RoundIterator(input, offset);
    }

    private class RoundIterator implements Iterator<T> {

        private final Collection<T> collection;
        private int remaining;

        private Iterator<T> iteratorOriginal;

        private RoundIterator(Collection<T> collection, int offset) {
            this.collection = collection;
            this.iteratorOriginal = collection.iterator();
            this.remaining = collection.size();
            for (int i = 0; i < offset; i++) {
                this.iteratorOriginal.next();
            }
        }

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        public T next() {
            if (remaining <= 0) throw new NoSuchElementException();

            remaining--;
            if (!iteratorOriginal.hasNext()) {
                iteratorOriginal = collection.iterator();
            }

            return iteratorOriginal.next();
        }
    }

}
