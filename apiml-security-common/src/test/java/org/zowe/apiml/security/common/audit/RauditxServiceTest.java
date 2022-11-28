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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class RauditxServiceTest {

    private static final String FMID = "FMIDTST";
    private static final String COMPONENT = "COMPTST";
    private static final int SUBTYPE = 2;
    private static final int EVENT = 2;
    private static final int QUALIFIER_SUCCESS = 0;
    private static final int QUALIFIER_FAILED = 1;

    private RauditxService rauditxService;
    private Rauditx mockRauditx;

    @BeforeEach
    void setUp() {
        mockRauditx = null;
        rauditxService = new RauditxService() {
            @Override
            Rauditx createMock() {
                if (mockRauditx != null) return mockRauditx;
                return super.createMock();
            }
        };
        ReflectionTestUtils.setField(rauditxService, "fmid", FMID);
        ReflectionTestUtils.setField(rauditxService, "component", COMPONENT);
        ReflectionTestUtils.setField(rauditxService, "subtype", SUBTYPE);
        ReflectionTestUtils.setField(rauditxService, "event", EVENT);
        ReflectionTestUtils.setField(rauditxService, "qualifierSuccess", QUALIFIER_SUCCESS);
        ReflectionTestUtils.setField(rauditxService, "qualifierFailed", QUALIFIER_FAILED);
    }

    @Test
    void givenService_whenCreateMock_thenReturnInstanceWithCallableMethods() {
        Rauditx rauditx = rauditxService.createMock();
        assertDoesNotThrow(() -> rauditx.addRelocateSection(5, new byte[7]));
        assertDoesNotThrow(rauditx::issue);
    }

    @Test
    void givenService_whenBuilder_thenReturnInstance() {
        assertNotNull(rauditxService.builder());
        assertNotNull(rauditxService.builder().rauditx);
    }

    @Test
    void givenService_whenBuilder_thenReturnBuilderWithDefaultValues() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder());

        verify(mockRauditx).setSubtype(SUBTYPE);
        verify(mockRauditx).setEvent(EVENT);
        verify(mockRauditx).setComponent(COMPONENT);
        verify(mockRauditx).setFmid(FMID);
    }

    @Test
    void givenBuilder_whenCallSuccess_thenSetEventAndDistribute() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().success());

        verify(mockRauditx).setEventSuccess();
        verify(mockRauditx).setQualifier(QUALIFIER_SUCCESS);
    }

    @Test
    void givenBuilder_whenCallFailure_thenSetEventAndDistribute() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().failure());

        verify(mockRauditx).setEventFailure();
        verify(mockRauditx).setQualifier(QUALIFIER_FAILED);
    }

    @Test
    void givenBuilder_whenCallAuthentication_thenRecalledToRauditx() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().authentication());
        verify(mockRauditx).setAuthenticationEvent();
    }

    @Test
    void givenBuilder_whenCallAuthorization_thenRecalledToRauditx() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().authorization());
        verify(mockRauditx).setAuthorizationEvent();
    }

    @Test
    void givenBuilder_whenCallAlwaysLogSuccesses_thenRecalledToRauditx() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().alwaysLogSuccesses());
        verify(mockRauditx).setAlwaysLogSuccesses();
    }

    @Test
    void givenBuilder_whenCallNeverLogSuccesses_thenRecalledToRauditx() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().neverLogSuccesses());
        verify(mockRauditx).setNeverLogSuccesses();
    }

    @Test
    void givenBuilder_whenCallAlwaysLogFailures_thenRecalledToRauditx() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().alwaysLogFailures());
        verify(mockRauditx).setAlwaysLogFailures();
    }

    @Test
    void givenBuilder_whenCallNeverLogFailures_thenRecalledToRauditx() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().neverLogFailures());
        verify(mockRauditx).setNeverLogFailures();
    }

    @Test
    void givenBuilder_whenCallCheckWarningMode_thenRecalledToRauditx() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().checkWarningMode());
        verify(mockRauditx).setCheckWarningMode();
    }

    @Test
    void givenBuilder_whenCallIgnoreSuccessWithNoAuditLogRecord_thenRecalledToRauditx() {
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().ignoreSuccessWithNoAuditLogRecord(true));
        verify(mockRauditx).setIgnoreSuccessWithNoAuditLogRecord(true);
        assertNotNull(rauditxService.builder().ignoreSuccessWithNoAuditLogRecord(false));
        verify(mockRauditx).setIgnoreSuccessWithNoAuditLogRecord(false);
    }

    @Test
    void givenBuilder_whenCallLogString_thenRecalledToRauditx() {
        String logString = "the test logString value";
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().logString(logString));
        verify(mockRauditx).setLogString(logString);
    }

    @Test
    void givenBuilder_whenCallMessageSegment_thenRecalledToRauditx() {
        String messageSegment = "the test messageSegment value";
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().messageSegment(messageSegment));
        verify(mockRauditx).addMessageSegment(messageSegment);
    }

    @Test
    void givenBuilder_whenCallUserId_thenRecalledToRauditx() {
        String userId = "the test userId value";
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().userId(userId));
        verify(mockRauditx).addRelocateSection(103, userId);
    }

    @Test
    void givenBuilder_whenCallEvent_thenRecalledToRauditx() {
        int eventId = 12345;
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().event(eventId));
        verify(mockRauditx).setEvent(eventId);
    }

    @Test
    void givenBuilder_whenCallQualifier_thenRecalledToRauditx() {
        int qualifier = 456;
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().qualifier(qualifier));
        verify(mockRauditx).setQualifier(qualifier);
    }

    @Test
    void givenBuilder_whenCallSetType_thenRecalledToRauditx() {
        int type = 159;
        mockRauditx = mock(Rauditx.class);
        assertNotNull(rauditxService.builder().subtype(type));
        verify(mockRauditx).setSubtype(type);
    }

    @Test
    void givenNoPermission_whenIssue_thenHandleException() {
        mockRauditx = mock(Rauditx.class);
        doThrow(new RauditxException(1, 2, 3)).when(mockRauditx).issue();
        RauditxService.RauditBuilder builder = rauditxService.builder();

        assertDoesNotThrow(builder::issue);
        verify(mockRauditx).issue();
    }

    @Test
    void givenRauditx_whenIssue_thenIsProperlyIssued() {
        RauditxService.RauditBuilder builder = rauditxService.builder();
        assertDoesNotThrow(builder::issue);
    }

}