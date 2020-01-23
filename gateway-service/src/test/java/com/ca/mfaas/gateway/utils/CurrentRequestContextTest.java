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

import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Provides functions to lock, clear, and unlock CurrentRequestContext in a test
 * so there are no race conditions in parallel test execution.
 */
public abstract class CurrentRequestContextTest {
    private final static ReentrantLock currentRequestContext = new ReentrantLock();

    protected RequestContext ctx;

    public void lockAndClearRequestContext() {
        currentRequestContext.lock();
        RequestContext.testSetCurrentContext(null);
        ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.setResponse(new MockHttpServletResponse());
    }

    public void unlockRequestContext() {
        RequestContext.testSetCurrentContext(null);
        ctx.clear();
        currentRequestContext.unlock();
    }
}
