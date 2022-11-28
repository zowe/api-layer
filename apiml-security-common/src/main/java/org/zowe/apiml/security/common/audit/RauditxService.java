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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Slf4j
@Service
public class RauditxService {

    // documented types at https://www.ibm.com/docs/en/zos/2.2.0?topic=records-smf-record-type-83-subtype-2
    private static final int RELOCATED_RECORD_TYPE_BIND_USER = 103;

    @Value("${rauditx.fmid:AZWE001}")
    private String fmid;

    @Value("${rauditx.component:ZOWE}")
    private String component;

    // Description of subtypes at https://www.ibm.com/docs/en/zos/2.5.0?topic=records-record-type-83-security-events
    @Value("${rauditx.subtype:2}")
    private int subtype;

    // Events and qualifiers documentation at https://www.ibm.com/docs/en/zos/2.5.0?topic=descriptions-event-codes-event-code-qualifiers
    @Value("${raudit.event:2}")
    private int event;

    @Value("${rauditx.qualifier.success:0}")
    private int qualifierSuccess;

    @Value("${rauditx.qualifier.failed:1}")
    private int qualifierFailed;

    void setDefault(RauditBuilder builder) {
        builder.subtype(subtype).event(event);

        builder.rauditx.setComponent(component);
        builder.rauditx.setFmid(fmid);
    }

    Rauditx createMock() {
        return (Rauditx) Proxy.newProxyInstance(
            RauditxService.class.getClassLoader(),
            new Class[] { Rauditx.class },
            (Object proxy, Method method, Object[] args) -> null
        );
    }

    public RauditBuilder builder() {
        Rauditx rauditx = ClassOrDefaultProxyUtils.createProxy(
            Rauditx.class,
            "com.ibm.jzos.Rauditx",
            this::createMock,
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.jzos.RauditxException", RauditxException.class,
                "getSafReturnCode", "getRacfReturnCode", "getRacfReasonCode"
            )
        );

        RauditBuilder builder = new RauditBuilder(rauditx);
        setDefault(builder);
        return builder;
    }

    @RequiredArgsConstructor
    public class RauditBuilder {

        final Rauditx rauditx;

        public RauditBuilder success() {
            rauditx.setEventSuccess();
            qualifier(qualifierSuccess);
            return this;
        }

        public RauditBuilder failure() {
            rauditx.setEventFailure();
            qualifier(qualifierFailed);
            return this;
        }

        public RauditBuilder authentication() {
            rauditx.setAuthenticationEvent();
            return this;
        }

        public RauditBuilder authorization() {
            rauditx.setAuthorizationEvent();
            return this;
        }

        public RauditBuilder alwaysLogSuccesses() {
            rauditx.setAlwaysLogSuccesses();
            return this;
        }

        public RauditBuilder neverLogSuccesses() {
            rauditx.setNeverLogSuccesses();
            return this;
        }

        public RauditBuilder alwaysLogFailures() {
            rauditx.setAlwaysLogFailures();
            return this;
        }

        public RauditBuilder neverLogFailures() {
            rauditx.setNeverLogFailures();
            return this;
        }

        public RauditBuilder checkWarningMode() {
            rauditx.setCheckWarningMode();
            return this;
        }

        public RauditBuilder ignoreSuccessWithNoAuditLogRecord(boolean ignoreSuccessWithNoAuditLogRecord) {
            rauditx.setIgnoreSuccessWithNoAuditLogRecord(ignoreSuccessWithNoAuditLogRecord);
            return this;
        }

        public RauditBuilder logString(String logString) {
            rauditx.setLogString(logString);
            return this;
        }

        public RauditBuilder messageSegment(String messageSegment) {
            rauditx.addMessageSegment(messageSegment);
            return this;
        }

        public RauditBuilder userId(String userId) {
            rauditx.addRelocateSection(RELOCATED_RECORD_TYPE_BIND_USER, userId);
            return this;
        }

        public RauditBuilder event(int event) {
            rauditx.setEvent(event);
            return this;
        }

        public RauditBuilder qualifier(int qualifier) {
            rauditx.setQualifier(qualifier);
            return this;
        }

        public RauditBuilder subtype(int subtype) {
            rauditx.setSubtype(subtype);
            return this;
        }

        public void issue() {
            try {
                rauditx.issue();
            } catch (RauditxException re) {
                log.debug("Cannot issue RAuditX record", re);
            }
        }

    }

}
