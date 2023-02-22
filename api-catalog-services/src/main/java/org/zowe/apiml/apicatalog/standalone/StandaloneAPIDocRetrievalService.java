/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import lombok.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.services.status.APIDocRetrievalService;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(
    value = "apiml.catalog.standalone.enabled",
    havingValue = "true")
public class StandaloneAPIDocRetrievalService extends APIDocRetrievalService {

    public StandaloneAPIDocRetrievalService() {
        super(null, null, null);
    }

    @Override
    public ApiDocInfo retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        return null;
    }

    @Override
    public ApiDocInfo retrieveDefaultApiDoc(@NonNull String serviceId) {
        return null;
    }

    @Override
    public List<String> retrieveApiVersions(@NonNull String serviceId) {
        return Collections.emptyList();
    }

    @Override
    public String retrieveDefaultApiVersion(@NonNull String serviceId) {
        return null;
    }

}
