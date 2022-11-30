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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RauditxServiceTest {

    private static final String FMID = "FMIDTST";
    private static final String COMPONENT = "COMPTST";
    private static final int SUBTYPE = 2;
    private static final int EVENT = 2;
    private static final int QUALIFIER_SUCCESS = 0;
    private static final int QUALIFIER_FAILED = 1;

    private static final String USER_ID = "USERTST";

    private SafResourceAccessVerifying safResourceAccessVerifying;
    private RauditxService rauditxService;
    private Rauditx mockRauditx;

    @BeforeEach
    void setUp() {
        mockRauditx = null;
        safResourceAccessVerifying = mock(SafResourceAccessVerifying.class);
        rauditxService = spy(new RauditxService() {
            @Override
            Rauditx createMock() {
                if (mockRauditx != null) return mockRauditx;
                return super.createMock();
            }
            @Override
            SafResourceAccessVerifying getNativeSafResourceAccessVerifying() {
                return safResourceAccessVerifying;
            }
        });
        ReflectionTestUtils.setField(rauditxService, "fmid", FMID);
        ReflectionTestUtils.setField(rauditxService, "component", COMPONENT);
        ReflectionTestUtils.setField(rauditxService, "subtype", SUBTYPE);
        ReflectionTestUtils.setField(rauditxService, "event", EVENT);
        ReflectionTestUtils.setField(rauditxService, "qualifierSuccess", QUALIFIER_SUCCESS);
        ReflectionTestUtils.setField(rauditxService, "qualifierFailed", QUALIFIER_FAILED);
    }

    @Nested
    class RauditxWrap {

        @Nested
        class GivenBuilder {

            @Test
            void whenCallSuccess_thenSetEventAndDistribute() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().success());

                verify(mockRauditx).setEventSuccess();
                verify(mockRauditx).setQualifier(QUALIFIER_SUCCESS);
            }

            @Test
            void whenCallFailure_thenSetEventAndDistribute() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().failure());

                verify(mockRauditx).setEventFailure();
                verify(mockRauditx).setQualifier(QUALIFIER_FAILED);
            }

            @Test
            void whenCallAuthentication_thenRecalledToRauditx() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().authentication());
                verify(mockRauditx).setAuthenticationEvent();
            }

            @Test
            void whenCallAuthorization_thenRecalledToRauditx() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().authorization());
                verify(mockRauditx).setAuthorizationEvent();
            }

            @Test
            void whenCallAlwaysLogSuccesses_thenRecalledToRauditx() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().alwaysLogSuccesses());
                verify(mockRauditx).setAlwaysLogSuccesses();
            }

            @Test
            void whenCallNeverLogSuccesses_thenRecalledToRauditx() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().neverLogSuccesses());
                verify(mockRauditx).setNeverLogSuccesses();
            }

            @Test
            void whenCallAlwaysLogFailures_thenRecalledToRauditx() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().alwaysLogFailures());
                verify(mockRauditx).setAlwaysLogFailures();
            }

            @Test
            void whenCallNeverLogFailures_thenRecalledToRauditx() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().neverLogFailures());
                verify(mockRauditx).setNeverLogFailures();
            }

            @Test
            void whenCallCheckWarningMode_thenRecalledToRauditx() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().checkWarningMode());
                verify(mockRauditx).setCheckWarningMode();
            }

            @Test
            void whenCallIgnoreSuccessWithNoAuditLogRecord_thenRecalledToRauditx() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().ignoreSuccessWithNoAuditLogRecord(true));
                verify(mockRauditx).setIgnoreSuccessWithNoAuditLogRecord(true);
                assertNotNull(rauditxService.builder().ignoreSuccessWithNoAuditLogRecord(false));
                verify(mockRauditx).setIgnoreSuccessWithNoAuditLogRecord(false);
            }

            @Test
            void whenCallLogString_thenRecalledToRauditx() {
                String logString = "the test logString value";
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().logString(logString));
                verify(mockRauditx).setLogString(logString);
            }

            @Test
            void whenCallMessageSegment_thenRecalledToRauditx() {
                String messageSegment = "the test messageSegment value";
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().messageSegment(messageSegment));
                verify(mockRauditx).addMessageSegment(messageSegment);
            }

            @Test
            void whenCallUserId_thenRecalledToRauditx() {
                String userId = "the test userId value";
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().userId(userId));
                verify(mockRauditx).addRelocateSection(103, userId);
            }

            @Test
            void whenCallEvent_thenRecalledToRauditx() {
                int eventId = 12345;
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().event(eventId));
                verify(mockRauditx).setEvent(eventId);
            }

            @Test
            void whenCallQualifier_thenRecalledToRauditx() {
                int qualifier = 456;
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().qualifier(qualifier));
                verify(mockRauditx).setQualifier(qualifier);
            }

            @Test
            void whenCallSetType_thenRecalledToRauditx() {
                int type = 159;
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder().subtype(type));
                verify(mockRauditx).setSubtype(type);
            }

        }

        @Nested
        class CreatingBuilder {

            @Test
            void whenBuilder_thenReturnInstance() {
                assertNotNull(rauditxService.builder());
                assertNotNull(rauditxService.builder().rauditx);
            }

            @Test
            void whenBuilder_thenReturnBuilderWithDefaultValues() {
                mockRauditx = mock(Rauditx.class);
                assertNotNull(rauditxService.builder());

                verify(mockRauditx).setSubtype(SUBTYPE);
                verify(mockRauditx).setEvent(EVENT);
                verify(mockRauditx).setComponent(COMPONENT);
                verify(mockRauditx).setFmid(FMID);
            }

        }
    }

    @Nested
    class SecurityTestsOnZos {

        @Nested
        class NotEnoughPermissions {

            @Test
            void whenIssue_thenHandleException() {
                mockRauditx = mock(Rauditx.class);
                doThrow(new RauditxException(1, 2, 3)).when(mockRauditx).issue();
                RauditxService.RauditxBuilder builder = rauditxService.builder();

                assertDoesNotThrow(builder::issue);
                verify(mockRauditx).issue();
            }

            @Test
            void whenVerifyPrivileges_thenLogError() {
                doReturn(USER_ID).when(rauditxService).getCurrentUser();
                rauditxService.verifyPrivileges();
                verify(rauditxService).logNoPrivileges(USER_ID);
            }

        }

        @Nested
        class WithPermissions {

            @Test
            void whenIssue_thenIsProperlyIssued() {
                RauditxService.RauditxBuilder builder = rauditxService.builder();
                assertDoesNotThrow(builder::issue);
            }

            @Test
            void whenVerifyPrivileges_thenDontLogAnyError() {
                doReturn(USER_ID).when(rauditxService).getCurrentUser();
                doReturn(true).when(safResourceAccessVerifying).hasSafResourceAccess(
                    argThat(x -> USER_ID.equals(x.getName())),
                    eq("FACILITY"), eq("IRR.RAUDITX"), eq("READ")
                );
                rauditxService.verifyPrivileges();
                verify(rauditxService, never()).logNoPrivileges(anyString());
            }

        }

    }

    @Nested
    class GivenNonZos {

        @Test
        void whenGetCurrentUser_thenReturnNull() {
            assertNull(rauditxService.getCurrentUser());
        }

        @Test
        void whenVerifyPrivileges_thenLogError() {
            rauditxService.verifyPrivileges();
            verify(rauditxService).logNoPrivileges(null);
        }

        @Test
        void whenGetNativeSafResourceAccessVerifying_thenReturnsNull() {
            assertNull(new RauditxService().getNativeSafResourceAccessVerifying());
        }

        @Test
        void whenCreateMock_thenReturnInstanceWithCallableMethods() {
            Rauditx rauditx = rauditxService.createMock();
            assertDoesNotThrow(() -> rauditx.addRelocateSection(5, new byte[7]));
            assertDoesNotThrow(rauditx::issue);
        }

    }

}