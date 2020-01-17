/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.utils;

import java.util.concurrent.locks.ReentrantLock;

import com.netflix.zuul.context.RequestContext;

import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Makes sure that only one test class derived from this class is using
 * RequestContext.getCurrentContext at a time
 */
public class CurrentRequestContextTest {
    private final static ReentrantLock currentRequestContext = new ReentrantLock();

    protected RequestContext ctx;

    @Before
    public void setUp() {
        currentRequestContext.lock();
        ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.setResponse(new MockHttpServletResponse());
    }

    @After
    public void tearDown() {
        currentRequestContext.unlock();
    }
}
