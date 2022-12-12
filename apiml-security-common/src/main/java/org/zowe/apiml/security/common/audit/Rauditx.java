/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.audit;

/**
 * This interface defines the same methods as the original JZos class to define the interface independent of JZos.
 *
 * Documentation of JZos implementation is available at
 * https://www.ibm.com/docs/api/v1/content/SSYKE2_8.0.0/com.ibm.java.zsecurity.api.80.doc/com.ibm.jzos/com/ibm/jzos/Rauditx.html
 */
public interface Rauditx {

    void setAttributes(long attributeBits);
    void setEventSuccess();
    void setEventFailure();
    void setAuthorizationEvent();
    void setAuthenticationEvent();
    void setAlwaysLogSuccesses();
    void setNeverLogSuccesses();
    void setAlwaysLogFailures();
    void setNeverLogFailures();
    void setCheckWarningMode();
    void setRacfClass(String racfClass);
    void setComponent(String component);
    void setEvent(int event);
    void setFmid(String fmid);
    void setIgnoreSuccessWithNoAuditLogRecord(boolean ignoreSuccessWithNoAuditLogRecord);
    void setLinkValue(byte[] bytes);
    void setLinkValue(String string);
    void setLogString(String logString);
    void setQualifier(int qualifier);
    void setResource(String resource);
    void setSubtype(int subtype);
    void addMessageSegment(String message);
    void addRelocateSection(int type, byte[] data);
    void addRelocateSection(int type, String string);
    void issue() throws RauditxException;

}
