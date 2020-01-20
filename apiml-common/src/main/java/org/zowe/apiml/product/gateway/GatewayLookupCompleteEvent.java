/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.gateway;

import org.springframework.context.ApplicationEvent;

import javax.validation.constraints.NotNull;

public class GatewayLookupCompleteEvent extends ApplicationEvent {

    /**
     * Event that occurs when {@link GatewayInstanceInitializer} finds the Gateway instance.
     *
     * @param source the object on which the event initially occurred
     */
    public GatewayLookupCompleteEvent(@NotNull Object source) {
        super(source);
    }
}
