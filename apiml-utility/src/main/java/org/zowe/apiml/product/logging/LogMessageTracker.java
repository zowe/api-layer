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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class that contains the content made by a given logger.
 * Utility functions are available to search for and confirm general or specific log entries and events.
 */
public class LogMessageTracker {
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    /**
     * @param loggedClass Class that generates the logs to be searched.
     */
    public LogMessageTracker(Class<?> loggedClass) {
        this(loggedClass.getName());
    }

    /**
     * @param loggerName String name of the logger that generates the logs to be searched. This can be the
     *                   package of the class that generates logs, or a specific name like Logger.ROOT_LOGGER_NAME.
     */
    public LogMessageTracker(String loggerName) {
        logAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        Logger log = (Logger) LoggerFactory.getLogger(loggerName);
        log.setLevel(Level.ALL);
        log.addAppender(logAppender);
    }

    /**
     * Method used to start log tracking, avoiding memory consumption when tracking isn't wanted.
     * Must be invoked before log tracking will work.
     */
    public void startTracking() {
        logAppender.start();
    }

    /**
     * Method used to lower memory usage. Clears the tracked logs and stops log tracking until ListAppender.start is invoked.
     */
    public void stopTracking() {
        logAppender.list.clear();
        logAppender.stop();
    }

    /**
     * @param regex Pattern regex used to search for an ILoggingEvent message.
     * @return true if and only if the list contains an ILoggingEvent with content matching the given regex.
     */
    public boolean contains(Pattern regex) {
        return logAppender.list.stream().anyMatch(event -> regex.matcher(event.getFormattedMessage()).find());
    }

    /**
     * @param content log content for the ILoggingEvent to be searched for. Not case sensitive.
     * @return true if and only if the list contains an ILoggingEvent matching the content.
     */
    public boolean contains(String content) {
        return logAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains(content));
    }

    /**
     * @param regex Pattern regex used to search for an ILoggingEvent message.
     * @param level severity level for the ILoggingEvent to be searched for.
     * @return true if and only if the list contains an ILoggingEvent matching the content and severity.
     */
    public boolean contains(Pattern regex, Level level) {
        return logAppender.list.stream().anyMatch(
            event -> regex.matcher(event.getFormattedMessage()).find()
                && event.getLevel().equals(level));
    }

    /**
     * @param content log content for the ILoggingEvent to be searched for. Not case sensitive.
     * @param level   severity level for the ILoggingEvent to be searched for.
     * @return true if and only if the list contains an ILoggingEvent matching the content and severity.
     */
    public boolean contains(String content, Level level) {
        return logAppender.list.stream().anyMatch(event -> event.getFormattedMessage().contains(content) && event.getLevel().equals(level));
    }

    /**
     * @param regex Pattern regex used to search for log content.
     * @return List of ILoggingEvent matching the given content.
     */
    public List<ILoggingEvent> search(Pattern regex) {
        return logAppender.list.stream()
            .filter(event -> regex.matcher(event.getFormattedMessage()).find())
            .collect(Collectors.toList());
    }

    /**
     * @param content String used to search for log content. Not case sensitive.
     * @return List of ILoggingEvent matching the given content.
     */
    public List<ILoggingEvent> search(String content) {
        return logAppender.list.stream()
            .filter(event -> event.getFormattedMessage().contains(content))
            .collect(Collectors.toList());
    }

    /**
     * @param regex Pattern regex used to search for log content.
     * @param level severity level for be searched for.
     * @return List of ILoggingEvent matching the given content and severity level.
     */
    public List<ILoggingEvent> search(Pattern regex, Level level) {
        return logAppender.list.stream()
            .filter(event -> regex.matcher(event.getFormattedMessage()).find()
                && event.getLevel().equals(level))
            .collect(Collectors.toList());
    }

    /**
     * @param content String used to search for log content. Not case sensitive.
     * @param level   severity level for be searched for.
     * @return List of ILoggingEvent matching the given content and severity level.
     */
    public List<ILoggingEvent> search(String content, Level level) {
        return logAppender.list.stream()
            .filter(event -> event.getFormattedMessage().contains(content)
                && event.getLevel().equals(level))
            .collect(Collectors.toList());
    }

    /**
     * @return the number (as a long) of ILoggingEvent generated.
     */
    public long countEvents() {
        return logAppender.list.size();
    }

    /**
     * @return unmodifiable List of the ILoggingEvent that were generated.
     */
    public List<ILoggingEvent> getAllLoggedEvents() {
        return Collections.unmodifiableList(logAppender.list);
    }

    /**
     * @param level Level used to filter for ILoggingEvent instances.
     * @return List of ILoggingEvent with a matching Level to the one given.
     */
    public List<ILoggingEvent> getAllLoggedEventsWithLevel(Level level) {
        return logAppender.list.stream()
            .filter(event -> event.getLevel().equals(level))
            .collect(Collectors.toList());
    }
}
