/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cache;

/**
 * Names and limits of the personal access token revocation store, shared by the issuer (ZAAS) and the store
 * itself (caching service).
 */
public final class PatRevocationStore {

    public static final String INVALID_TOKENS_KEY = "invalidTokens";

    public static final String INVALID_USERS_KEY = "invalidUsers";

    public static final String INVALID_SCOPES_KEY = "invalidScopes";

    /**
     * How long a revocation rule stays relevant. A personal access token lives at most 90 days, so a rule
     * older than that cannot govern any token that is still valid.
     */
    public static final int RULE_RETENTION_DAYS = 90;

    /**
     * Maximum number of scopes a single personal access token may be issued with.
     */
    public static final int DEFAULT_MAX_SCOPES_PER_TOKEN = 64;

    /**
     * Maximum number of keys a single {@code /cache-query} request may ask for.
     */
    public static final int DEFAULT_MAX_QUERY_KEYS = DEFAULT_MAX_SCOPES_PER_TOKEN + 2;

    /**
     * A revocation rule reads "invalidate every token created at or before this instant", and its own
     * retention is derived from the same instant.
     */
    public static final long DEFAULT_RULE_TIMESTAMP_SKEW_ALLOWANCE_MILLIS = 60_000L;

    private PatRevocationStore() {
        // constants only
    }
}
