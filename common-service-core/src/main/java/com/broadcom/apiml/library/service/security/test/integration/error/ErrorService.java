/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.test.integration.error;

import com.broadcom.apiml.library.service.security.test.integration.error.impl.ErrorServiceImpl;
import com.broadcom.apiml.library.service.security.test.integration.rest.response.ApiMessage;
import com.broadcom.apiml.library.service.security.test.integration.rest.response.Message;

import java.util.List;

/**
 * Service for creating {@link ApiMessage} by string key and list of paramets.
 * See default implementation {@link ErrorServiceImpl}.
 */
public interface ErrorService {
    /**
     * Create {@link ApiMessage} that contains one {@link Message}
     * for provided key with array of parameters.
     *
     * @param key        of message in messages.yml file
     * @param parameters for message
     * @return {@link ApiMessage} for key
     */
    ApiMessage createApiMessage(String key, Object... parameters);

    /**
     * Create {@link ApiMessage} that contains list of {@link Message}
     * with same key and provided parameters.
     *
     * @param key        of message in messages.yml file
     * @param parameters list that contains arrays of parameters
     * @return {@link ApiMessage} for key
     */
    ApiMessage createApiMessage(String key, List<Object[]> parameters);
}
