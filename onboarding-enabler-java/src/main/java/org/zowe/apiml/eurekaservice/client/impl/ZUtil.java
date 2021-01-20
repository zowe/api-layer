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

public interface ZUtil {

    String[] environ();
    String formatStackTrace(Throwable t);
    String getCodePageCurrentLocale();
    long getCpuTimeMicros();
    String getCurrentJobId();
    String getCurrentJobname();
    String getCurrentProcStepname();
    String getCurrentStepname();
    long getCurrentTimeMicros();
    String getCurrentTsoPrefix();
    String getCurrentUser();
    String getDefaultPlatformEncoding();
    String getEnv(String varName);
    Properties getEnvironment();
    String getJavaVersionInfo();
    String getJzosDllVersion();
    String getJzosJarVersion();
    int getLoggingLevel();
    int getPid();
    int getPPid();
    byte[] getTodClock();
    void getTodClock(byte[] buffer);
    byte[] getTodClockExtended();
    void getTodClockExtended(byte[] buffer);
    void logDiagnostic(int level, String msg);
    PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush);
    PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush, String encoding);
    PrintStream newEncodedPrintStream(OutputStream os, boolean autoFlush, String encoding, boolean enable);
    void peekOSMemory(long address, byte[] bytes);
    void peekOSMemory(long address, byte[] bytes, int offset, int len);
    long peekOSMemory(long address, int len);
    void redirectStandardStreams();
    boolean redirectStandardStreams(String requestedEncoding, boolean enableTranscoding);
    void setDefaultPlatformEncoding(String encoding);
    void setEnv(String varName, String varValue);
    void setLoggingLevel(int level);
    void smfRecord(int type, int subtype, byte[] record);
    String substituteSystemSymbols(String pattern);
    String substituteSystemSymbols(String pattern, boolean warn);
    void touch();

}
