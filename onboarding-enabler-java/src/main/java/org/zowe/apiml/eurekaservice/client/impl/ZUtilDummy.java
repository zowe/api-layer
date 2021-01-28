/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.eurekaservice.client.impl;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

public class ZUtilDummy implements ZUtil {

    @Override
    public String[] environ() {
        return new String[0];
    }

    @Override
    public String formatStackTrace(Throwable t) {
        return null;
    }

    @Override
    public String getCodePageCurrentLocale() {
        return null;
    }

    @Override
    public long getCpuTimeMicros() {
        return 0;
    }

    @Override
    public String getCurrentJobId() {
        return null;
    }

    @Override
    public String getCurrentJobname() {
        return null;
    }

    @Override
    public String getCurrentProcStepname() {
        return null;
    }

    @Override
    public String getCurrentStepname() {
        return null;
    }

    @Override
    public long getCurrentTimeMicros() {
        return 0;
    }

    @Override
    public String getCurrentTsoPrefix() {
        return null;
    }

    @Override
    public String getCurrentUser() {
        return null;
    }

    @Override
    public String getDefaultPlatformEncoding() {
        return null;
    }

    @Override
    public String getEnv(String varName) {
        return null;
    }

    @Override
    public Properties getEnvironment() {
        return null;
    }

    @Override
    public String getJavaVersionInfo() {
        return null;
    }

    @Override
    public String getJzosDllVersion() {
        return null;
    }

    @Override
    public String getJzosJarVersion() {
        return null;
    }

    @Override
    public int getLoggingLevel() {
        return 0;
    }

    @Override
    public int getPid() {
        return 0;
    }

    @Override
    public int getPPid() {
        return 0;
    }

    @Override
    public byte[] getTodClock() {
        return new byte[0];
    }

    @Override
    public void getTodClock(byte[] buffer) {
        // dummy implementation - do nothing
    }

    @Override
    public byte[] getTodClockExtended() {
        return new byte[0];
    }

    @Override
    public void getTodClockExtended(byte[] buffer) {
        // dummy implementation - do nothing
    }

    @Override
    public void logDiagnostic(int level, String msg) {
        // dummy implementation - do nothing
    }

    @Override
    public PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush) {
        return null;
    }

    @Override
    public PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush, String encoding) {
        return null;
    }

    @Override
    public PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush, String encoding, boolean enable) {
        return null;
    }

    @Override
    public void peekOSMemory(long address, byte[] bytes) {
        // dummy implementation - do nothing
    }

    @Override
    public void peekOSMemory(long address, byte[] bytes, int offset, int len) {
        // dummy implementation - do nothing
    }

    @Override
    public long peekOSMemory(long address, int len) {
        return 0;
    }

    @Override
    public void redirectStandardStreams() {
        // dummy implementation - do nothing
    }

    @Override
    public boolean redirectStandardStreams(String requestedEncoding, boolean enableTranscoding) {
        return false;
    }

    @Override
    public void setDefaultPlatformEncoding(String encoding) {
        // dummy implementation - do nothing
    }

    @Override
    public void setEnv(String varName, String varValue) {
        // dummy implementation - do nothing
    }

    @Override
    public void setLoggingLevel(int level) {
        // dummy implementation - do nothing
    }

    @Override
    public void smfRecord(int type, int subtype, byte[] record) {
        // dummy implementation - do nothing
    }

    @Override
    public String substituteSystemSymbols(String pattern) {
        return null;
    }

    @Override
    public String substituteSystemSymbols(String pattern, boolean warn) {
        return null;
    }

    @Override
    public void touch() {
        // dummy implementation - do nothing
    }

}
