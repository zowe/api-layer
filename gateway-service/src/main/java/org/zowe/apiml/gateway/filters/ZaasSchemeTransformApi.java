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

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;

@Service
@ConditionalOnBean(name = "modulithConfig")
public class ZaasSchemeTransformApi implements ZaasSchemeTransform {

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<TicketResponse>> passticket(RequestCredentials requestCredentials) {
        return null;
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> safIdt(RequestCredentials requestCredentials) {
        return null;
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> zosmf(RequestCredentials requestCredentials) {
        return null;
    }

    @Override
    public Mono<AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse>> zoweJwt(RequestCredentials requestCredentials) {
        return null;
    }

}
