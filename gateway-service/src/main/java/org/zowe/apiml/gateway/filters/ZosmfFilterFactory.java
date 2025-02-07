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

import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;


@Service
public class ZosmfFilterFactory extends AbstractTokenFilterFactory<AbstractTokenFilterFactory.Config> {

    private final ZaasSchemeTransform zaasSchemeTransform;

    public ZosmfFilterFactory(ZaasSchemeTransform zaasSchemeTransform, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(AbstractTokenFilterFactory.Config.class, instanceInfoService, messageService);
        this.zaasSchemeTransform = zaasSchemeTransform;
    }

    @Override
    protected Function<RequestCredentials, Mono<AuthorizationResponse<ZaasTokenResponse>>> getAuthorizationResponseTransformer() {
        return zaasSchemeTransform::zosmf;
    }

}
