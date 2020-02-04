/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger.api.dummy;

import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.swagger.api.AbstractApiDocService;
import org.zowe.apiml.product.gateway.GatewayClient;

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
