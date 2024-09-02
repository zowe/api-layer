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
import org.springframework.stereotype.Component;

/**
 * This filter should run on all requests for services, which do not have enabled encoded characters in URL
 * <p>
 * Special characters encoding is enabled on Tomcat so this filter takes over responsibility
 * for filtering them.
 * Encoded characters in URL are allowed by default.
 */

@Component
public class ForbidEncodedCharactersFilterFactory extends AbstractEncodedCharactersFilterFactory {

    private static final char[] PROHIBITED_CHARACTERS = {'%', ';', '\\'};

    public ForbidEncodedCharactersFilterFactory() {
        super();
    }

    @Override
    protected boolean shouldFilter(String uri) {
        return StringUtils.containsAny(uri, PROHIBITED_CHARACTERS);
    }

    RuntimeException getException(String uri) {
        return new ForbidCharacterException(uri);
    }
}
