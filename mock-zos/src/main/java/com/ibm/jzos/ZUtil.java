/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ibm.jzos;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

public class ZUtil {

    public String[] environ() {
        return new String[0];
    }

    public String formatStackTrace(Throwable t) {
        return null;
    }

    public String getCodePageCurrentLocale() {
        return null;
    }

    public long getCpuTimeMicros() {
        return 0;
    }

    public String getCurrentJobId() {
        return null;
    }

    public String getCurrentJobname() {
        return null;
    }

    public String getCurrentProcStepname() {
        return null;
    }

    public String getCurrentStepname() {
        return null;
    }

    public long getCurrentTimeMicros() {
        return 0;
    }

    public String getCurrentTsoPrefix() {
        return null;
    }

    public String getCurrentUser() {
        return null;
    }

    public String getDefaultPlatformEncoding() {
        return null;
    }

    public String getEnv(String varName) {
        return null;
    }

    public Properties getEnvironment() {
        return null;
    }

    public String getJavaVersionInfo() {
        return null;
    }

    public String getJzosDllVersion() {
        return null;
    }

    public String getJzosJarVersion() {
        return null;
    }

    public int getLoggingLevel() {
        return 0;
    }

    public int getPid() {
        return 0;
    }

    public int getPPid() {
        return 0;
    }

    public byte[] getTodClock() {
        return new byte[0];
    }

    public void getTodClock(byte[] buffer) {
        // dummy implementation - do nothing
    }

    public byte[] getTodClockExtended() {
        return new byte[0];
    }

    public void getTodClockExtended(byte[] buffer) {
        // dummy implementation - do nothing
    }

    public void logDiagnostic(int level, String msg) {
        // dummy implementation - do nothing
    }

    public PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush) {
        return null;
    }

    public PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush, String encoding) {
        return null;
    }

    public PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush, String encoding, boolean enable) {
        return null;
    }

    public void peekOSMemory(long address, byte[] bytes) {
        // dummy implementation - do nothing
    }

    public void peekOSMemory(long address, byte[] bytes, int offset, int len) {
        // dummy implementation - do nothing
    }

    public long peekOSMemory(long address, int len) {
        return 0;
    }

    public void redirectStandardStreams() {
        // dummy implementation - do nothing
    }

    public boolean redirectStandardStreams(String requestedEncoding, boolean enableTranscoding) {
        return false;
    }

    public void setDefaultPlatformEncoding(String encoding) {
        // dummy implementation - do nothing
    }

    public void setEnv(String varName, String varValue) {
        // dummy implementation - do nothing
    }

    public void setLoggingLevel(int level) {
        // dummy implementation - do nothing
    }

    public void smfRecord(int type, int subtype, byte[] rec) {
        // dummy implementation - do nothing
    }

    public String substituteSystemSymbols(String pattern) {
        return null;
    }

    public String substituteSystemSymbols(String pattern, boolean warn) {
        return null;
    }

    public void touch() {
        // dummy implementation - do nothing
    }
}
