/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.zowe.apiml.cache.EntryExpiration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RetryIfExpiredAspectTest {

    public Object call(Object first, Object second) throws Throwable {
        Object[] args = new Object[0];
        ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);

        doReturn(args).when(proceedingJoinPoint).getArgs();
        doAnswer(new Answer<Object>() {

            int counter;

            @Override
            public Object answer(InvocationOnMock invocation) {
                switch (counter++) {
                    case 0: return first;
                    case 1: return second;
                    default:
                        fail("Unexpected call");
                        return null;
                }
            }

        }).when(proceedingJoinPoint).proceed(args);

        return new RetryIfExpiredAspect().process(proceedingJoinPoint);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNonEntryExpirationValue_whenCallService_thenThrowException() throws Throwable {
        call(new Object(), null);
    }

    @Test
    public void givenNull_whenCallService_thenReturnNull() throws Throwable {
        assertNull(call(null, null));
    }

    @Test
    public void givenNonExpired_whenCallService_thenReturnFirst() throws Throwable {
        TestObject first = new TestObject(false);
        assertSame(first, call(first, null));
    }

    @Test
    public void givenExpired_whenCallService_thenReturnSecond() throws Throwable {
        TestObject first = new TestObject(true);
        TestObject second = new TestObject(false);
        assertSame(second, call(first, second));
    }

    @Test
    public void givenRetryIfExpiredAspect_whenGetOrder_thenNotLowestPrecedence() {
        assertTrue(new RetryIfExpiredAspect().getOrder() < Integer.MAX_VALUE);
    }

    @Getter
    @AllArgsConstructor
    private class TestObject implements EntryExpiration {

        private boolean expired;

    }

}
