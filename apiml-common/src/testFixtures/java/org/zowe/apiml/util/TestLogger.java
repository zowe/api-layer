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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class TestLogger extends AppenderBase<ILoggingEvent> implements Recorder<ILoggingEvent> {

    private static AtomicReference<TestLogger> lastInstance = new AtomicReference<>();

    private boolean listening;
    private List<ILoggingEvent> records;

    public TestLogger() {
        if (lastInstance.get() != null) {
            listening = lastInstance.get().listening;
            records = lastInstance.get().records;
        } else {
            records = new LinkedList<>();
        }
        lastInstance.set(this);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (listening) {
            records.add(eventObject);
        }
    }

    public static Recorder<ILoggingEvent> getHandler() {
        return lastInstance.getAndUpdate(
            x -> x == null ? new TestLogger() : x
        );
    }

    @Override
    public void startRecording() {
        listening = true;
    }

    @Override
    public void stopRecording() {
        records.clear();
        listening = false;
    }

    @Override
    public Optional<ILoggingEvent> findFirst(Predicate<ILoggingEvent> filter) {
        return records.stream().filter(filter).findFirst();
    }

    @Override
    public Optional<ILoggingEvent> findFirst(String messageInfix) {
        return records.stream().filter(x -> x.getMessage().contains(messageInfix)).findFirst();
    }

    @Override
    public List<ILoggingEvent> find(Predicate<ILoggingEvent> filter) {
        return records.stream().filter(filter).toList();
    }

    @Override
    public List<ILoggingEvent> find(String messageInfix) {
        return records.stream().filter(x -> x.getMessage().contains(messageInfix)).toList();
    }

}

