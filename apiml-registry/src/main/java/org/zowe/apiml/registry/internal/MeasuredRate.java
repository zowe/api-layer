/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.internal;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Counts events in the previous complete interval.
 * <p>
 * Self-preservation compares the number of renewals received in the last minute against a threshold, so what is
 * needed is the count for the <em>previous</em> window rather than a running total: a partially-elapsed current
 * minute would always look low and would suspend eviction permanently.
 * <p>
 * Deliberately not backed by a scheduled task. Eureka's equivalent used a timer, which made tests depend on
 * wall-clock sleeps; here the roll happens lazily on read against an injected clock, so the self-preservation
 * tests can drive time forward directly.
 */
public final class MeasuredRate {

    private final long intervalMs;
    private final AtomicLong currentBucket = new AtomicLong();
    private final AtomicLong lastBucket = new AtomicLong();
    private volatile long windowStart;

    public MeasuredRate(long intervalMs, long now) {
        this.intervalMs = intervalMs;
        this.windowStart = now;
    }

    public void increment(long now) {
        roll(now);
        currentBucket.incrementAndGet();
    }

    /** The count for the last complete interval. */
    public long lastCount(long now) {
        roll(now);
        return lastBucket.get();
    }

    private synchronized void roll(long now) {
        long elapsed = now - windowStart;
        if (elapsed < intervalMs) {
            return;
        }
        if (elapsed >= 2 * intervalMs) {
            // More than one whole interval passed with no activity - the previous window is genuinely empty.
            lastBucket.set(0);
            currentBucket.set(0);
        } else {
            lastBucket.set(currentBucket.getAndSet(0));
        }
        windowStart = now - (elapsed % intervalMs);
    }

}
