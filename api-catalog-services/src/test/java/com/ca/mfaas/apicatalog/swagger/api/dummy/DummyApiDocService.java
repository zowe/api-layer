/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.apicatalog.swagger.api.dummy;

import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.swagger.api.AbstractApiDocService;
import com.ca.mfaas.product.gateway.GatewayClient;

public class DummyApiDocService extends AbstractApiDocService {

    public DummyApiDocService(GatewayClient gatewayClient) {
        super(gatewayClient);
    }

    @Override
    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) {
        return null;
    }

    @Override
    protected void updatePaths(Object swaggerAPI, String serviceId, ApiDocInfo apiDocInfo, boolean hidden) {

    }

    @Override
    protected void updateExternalDoc(Object swaggerAPI, ApiDocInfo apiDocInfo) {

    }
}
