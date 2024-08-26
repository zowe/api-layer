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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.zowe.apiml.message.core.MessageService;

@Component
public class ForbidEncodedCharactersFilterFactory extends AbstractEncodedCharactersFilterFactory {

    private static final String[] PROHIBITED_CHARACTERS = {"%", ";", "\\"};

    public ForbidEncodedCharactersFilterFactory(MessageService messageService, ObjectMapper mapper, LocaleContextResolver localeContextResolver) {
        super(messageService, mapper, localeContextResolver, "org.zowe.apiml.gateway.requestContainEncodedCharacter");
    }

    @Override
    protected boolean shouldFilter(String uri) {
        return StringUtils.containsAny(uri, PROHIBITED_CHARACTERS);
    }

}
