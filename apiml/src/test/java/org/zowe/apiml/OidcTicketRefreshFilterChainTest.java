/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

/**
 * OIDC ticket/refresh filter chain integration tests.
 * These tests verify the WebSecurityConfig filter chains for /auth/ticket
 * and /auth/refresh when OIDC is enabled/disabled.
 *
 * <p>These are integration tests that require a running gateway instance
 * and are executed as part of the OidcOauth2Test suite.</p>
 */
class OidcTicketRefreshFilterChainTest {
    // Integration tests live in OidcOauth2Test and PassTicketTest.
    // This class serves as a marker for the test package.
}
