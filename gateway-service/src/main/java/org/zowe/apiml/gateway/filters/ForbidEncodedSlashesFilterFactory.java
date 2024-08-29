/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * This filter checks if encoded slashes in the URI are allowed based on configuration.
 * If not allowed and encoded slashes are present, it returns a BAD_REQUEST response.
 */
@Component
@ConditionalOnProperty(name = "apiml.service.allowEncodedSlashes", havingValue = "false", matchIfMissing = true)
public class ForbidEncodedSlashesFilterFactory extends AbstractEncodedCharactersFilterFactory {

    private static final String ENCODED_SLASH = "%2f";

    public ForbidEncodedSlashesFilterFactory() {
        super();
    }

    @Override
    protected boolean shouldFilter(String uri) {
        return StringUtils.containsIgnoreCase(uri, ENCODED_SLASH);
    }

    @Override
    RuntimeException getException(String uri) {
        return new ForbidSlashException(uri);
    }
}
