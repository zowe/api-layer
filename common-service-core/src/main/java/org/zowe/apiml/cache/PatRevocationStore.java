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
 * <p>
 * The map names are here rather than duplicated on both sides because the caching service derives an entry's
 * expiration from the map it is written to - a rename on one side only would silently store rules forever.
 * The two limits are derived from a single constant for the same reason: a PAT validation looks up
 * {@code scopes + 2} hashes (the token hash and the user hash), so a batch-lookup limit lower than the
 * issuance cap would make a legitimately-issued token permanently unauthenticatable -
 * {@code PATAuthSourceService.isValid} fails closed on the resulting error, and there is no recovery short of
 * issuing a new PAT.
 * <p>
 * Both limits are also exposed as operator-facing configuration; these values are only the defaults. Changing
 * one here means changing it in four more places, none of which the compiler checks: the {@code -D} in each
 * component's {@code bin/start.sh}, the default under {@code configs:} in its {@code manifest.yaml}, and the
 * typed entry in its {@code schemas/*-config.json}.
 */
public final class PatRevocationStore {

    /** Map of revoked individual tokens: hashed token -&gt; serialized {@code AccessTokenContainer}. */
    public static final String INVALID_TOKENS_KEY = "invalidTokens";

    /** Map of per-user revocation rules: hashed user id -&gt; revocation timestamp in epoch millis. */
    public static final String INVALID_USERS_KEY = "invalidUsers";

    /** Map of per-service revocation rules: hashed service id -&gt; revocation timestamp in epoch millis. */
    public static final String INVALID_SCOPES_KEY = "invalidScopes";

    /**
     * How long a revocation rule stays relevant. A personal access token lives at most 90 days, so a rule
     * older than that cannot govern any token that is still valid.
     */
    public static final int RULE_RETENTION_DAYS = 90;

    /**
     * Maximum number of scopes a single personal access token may be issued with. Scopes are service ids, so
     * the practical ceiling is "how many services one token is scoped to"; this is set comfortably above
     * anything a deployment is expected to use. Raising it is safe; lowering it below what a site has already
     * issued is not - already-issued tokens above the cap start failing the batch lookup immediately.
     */
    public static final int DEFAULT_MAX_SCOPES_PER_TOKEN = 64;

    /**
     * Maximum number of keys a single {@code /cache-query} request may ask for. Must stay at least
     * {@link #DEFAULT_MAX_SCOPES_PER_TOKEN} + 2.
     */
    public static final int DEFAULT_MAX_QUERY_KEYS = DEFAULT_MAX_SCOPES_PER_TOKEN + 2;

    private PatRevocationStore() {
        // constants only
    }
}
