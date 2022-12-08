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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessSaf;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Proxy;

/**
 * RauditxService offer issuing SMF record #83 via RauditX macro. Those records should be used to audit user action
 * that does not call on the end ESM macro to verify credentials (they are audited in the ESM). It means this audit
 * records should be generated for example when a token allows to generate another token. In this case ESM is not
 * called.
 *
 * To use this feature the calling userid must have READ authority to the IRR.RAUDITX profile in the FACILITY class.
 * On the initialization of this bean the privileges are checked and could write a warning message in the console log.
 *
 * In case this feature is not available (not enough credentials, the service runs off z/OS) it will not throw any
 * exception. The code should not check the possibility to issue SMF record neither.
 *
 * Example:
 *
 * <pre>
 * &#64;Service
 * &#64;RequiredArgsConstructor
 * class AuditedClass {
 *
 *  private final RauditxService rauditxService;
 *
 *  void doSomething() {
 *      ...
 *      rauditxService.builder().
 *          .userId("userId")
 *          .messageSegment("An attempt to generate PAT")
 *          .alwaysLogSuccesses()
 *          .alwaysLogFailures()
 *          .issue();
 *      ...
 *  }
 *
 * }
 * </pre>
 */
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
    @Value("${rauditx.event:2}")
    private int event;

    @Value("${rauditx.qualifier.success:0}")
    private int qualifierSuccess;

    @Value("${rauditx.qualifier.failed:1}")
    private int qualifierFailed;

    String getCurrentUser() {
        try {
            Class<?> zutilClass = Class.forName("com.ibm.jzos.ZUtil");
            MethodHandle getCurrentUser = MethodHandles.publicLookup().findStatic(zutilClass, "getCurrentUser", MethodType.methodType(String.class));
            return (String) getCurrentUser.invoke();
        } catch (Throwable t) {
            log.debug("Cannot obtain current userId", t);
            return null;
        }
    }

    void logNoPrivileges(String userId) {
        if (StringUtils.isBlank(userId)) {
            log.debug("Cannot issue any Rauditx record off z/OS.");
        } else {
            log.warn("The calling userid ({}) must have READ authority to the IRR.RAUDITX profile in the FACILITY class to issue a Rauditx record.", userId);
        }
    }

    SafResourceAccessVerifying getNativeSafResourceAccessVerifying() {
        try {
            return new SafResourceAccessSaf();
        } catch (Exception e) {
            log.debug("Cannot create instance of SafResourceAccessSaf");
            return null;
        }
    }

    @PostConstruct
    public void verifyPrivileges() {
        String userId = getCurrentUser();
        boolean hasAccess = false;
        if (!StringUtils.isBlank(userId)) {
            SafResourceAccessVerifying safResourceAccessVerifying = getNativeSafResourceAccessVerifying();
            if (safResourceAccessVerifying != null) {
                hasAccess = safResourceAccessVerifying.hasSafResourceAccess(
                    new UsernamePasswordAuthenticationToken(userId, null),
                    "FACILITY", "IRR.RAUDITX", "READ"
                );
            }
        }
        if (!hasAccess) {
            logNoPrivileges(userId);
        }
    }

    void setDefault(RauditxBuilder builder) {
        builder.subtype(subtype).event(event);

        builder.rauditx.setComponent(component);
        builder.rauditx.setFmid(fmid);
    }

    Rauditx createMock() {
        return (Rauditx) Proxy.newProxyInstance(
            RauditxService.class.getClassLoader(),
            new Class[] { Rauditx.class },
            (proxy, method, args) -> null
        );
    }

    /**
     * Returns the builder to define attributes or the record. The builder sets a couple of default values:
     *  - subtype
     *    - configurable by configuration property `rauditx.subtype`
     *    - the default value is `2`
     *  - event
     *    - configurable by configuration property `rauditx.event`
     *    - the default value is `2`
     *  - component
     *    - configurable by configuration property `rauditx.component`
     *    - the default value is `ZOWE`
     *  - FMID
     *    - configurable by configuration property `rauditx.fmid`
     *    - the default value is `AZWE001`
     *
     * All values above could be overridden via the builder.
     *
     * @return The builder of Rauditx record
     */
    public RauditxBuilder builder() {
        Rauditx rauditx = ClassOrDefaultProxyUtils.createProxy(
            Rauditx.class,
            "com.ibm.jzos.Rauditx",
            this::createMock,
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.jzos.RauditxException", RauditxException.class,
                "getSafReturnCode", "getRacfReturnCode", "getRacfReasonCode"
            )
        );

        RauditxBuilder builder = new RauditxBuilder(rauditx);
        setDefault(builder);
        return builder;
    }

    @RequiredArgsConstructor
    public class RauditxBuilder {

        final Rauditx rauditx;

        /**
         * Mark the audit record as successful. It also set qualifier to value set via property
         * `rauditx.qualifier.success`, as default `0`.
         * @return builder to next action
         */
        public RauditxBuilder success() {
            rauditx.setEventSuccess();
            qualifier(qualifierSuccess);
            return this;
        }

        /**
         * Mark the audit record as failed. It also set qualifier to value set via property
         * `rauditx.qualifier.failed`, as default `1`.
         * @return builder to next action
         */
        public RauditxBuilder failure() {
            rauditx.setEventFailure();
            qualifier(qualifierFailed);
            return this;
        }

        /**
         * Set the reason of audit record.
         * @return builder to next action
         */
        public RauditxBuilder authentication() {
            rauditx.setAuthenticationEvent();
            return this;
        }

        /**
         * Set the reason of audit record.
         * @return builder to next action
         */
        public RauditxBuilder authorization() {
            rauditx.setAuthorizationEvent();
            return this;
        }

        /**
         * Set the callable service to always log successes.
         * @return builder to next action
         */
        public RauditxBuilder alwaysLogSuccesses() {
            rauditx.setAlwaysLogSuccesses();
            return this;
        }

        /**
         * Set the callable service to never log successes.
         * @return builder to next action
         */
        public RauditxBuilder neverLogSuccesses() {
            rauditx.setNeverLogSuccesses();
            return this;
        }

        /**
         * Set the callable service to always log failures.
         * @return builder to next action
         */
        public RauditxBuilder alwaysLogFailures() {
            rauditx.setAlwaysLogFailures();
            return this;
        }

        /**
         * Set the callable service to never log failures.
         * @return builder to next action
         */
        public RauditxBuilder neverLogFailures() {
            rauditx.setNeverLogFailures();
            return this;
        }

        /**
         * Set the callable service check warning mode.
         * @return builder to next action
         */
        public RauditxBuilder checkWarningMode() {
            rauditx.setCheckWarningMode();
            return this;
        }

        /**
         * Sets a flag to not throw an exception when the R_auditx callable service is successful, but no audit record
         * is logged.
         * @param ignoreSuccessWithNoAuditLogRecord set `true` to ignore
         * @return builder to next action
         */
        public RauditxBuilder ignoreSuccessWithNoAuditLogRecord(boolean ignoreSuccessWithNoAuditLogRecord) {
            rauditx.setIgnoreSuccessWithNoAuditLogRecord(ignoreSuccessWithNoAuditLogRecord);
            return this;
        }

        /**
         * Sets the log string - character data to be written with the audit information.
         * @param logString a String between 1 and 255 characters.
         * @return builder to next action
         */
        public RauditxBuilder logString(String logString) {
            rauditx.setLogString(logString);
            return this;
        }

        /**
         * Add a message to be written to the console on Event Failure. The first message segment added should begin
         * with a component message identifier of 15 characters or less.
         * @param messageSegment a String between 1 and 70 characters
         * @return builder to next action
         */
        public RauditxBuilder messageSegment(String messageSegment) {
            rauditx.addMessageSegment(messageSegment);
            return this;
        }

        /**
         * Set binded userId.
         * @param userId binded userId to be audited
         * @return builder to next action
         */
        public RauditxBuilder userId(String userId) {
            rauditx.addRelocateSection(RELOCATED_RECORD_TYPE_BIND_USER, userId);
            return this;
        }

        /**
         * Set the event code (https://www.ibm.com/docs/en/zos/2.5.0?topic=descriptions-event-codes-event-code-qualifiers).
         * As default set to `2` or value configured by property `rauditx.event`.
         * @param event the event code int between 1 and 255
         * @return builder to next action
         */
        public RauditxBuilder event(int event) {
            rauditx.setEvent(event);
            return this;
        }

        /**
         * Set the event code (https://www.ibm.com/docs/en/zos/2.5.0?topic=descriptions-event-codes-event-code-qualifiers).
         * This value could be set by methods {@link #success} and {@link #failure()}. Be aware the this method is called
         * after or without them, otherwise the value will be overriden.
         * @param qualifier the event code qualifier int between 0 and 255
         * @return builder to next action
         */
        public RauditxBuilder qualifier(int qualifier) {
            rauditx.setQualifier(qualifier);
            return this;
        }

        /**
         * Sets the SMF type 83 record subtype assigned to the component (https://www.ibm.com/docs/en/zos/2.5.0?topic=records-record-type-83-security-events).
         * As default set to `2` or value configured by property `rauditx.subtype`.
         * @param subtype an int between 2 and 32767
         * @return builder to next action
         */
        public RauditxBuilder subtype(int subtype) {
            rauditx.setSubtype(subtype);
            return this;
        }

        /**
         * Issue the call to the R_auditx callable service. The method does not throw any exception. The error could
         * be written in the console log (level debug).
         */
        public void issue() {
            try {
                rauditx.issue();
            } catch (RauditxException re) {
                log.debug("Cannot issue RAuditX record", re);
            }
        }

    }

}
