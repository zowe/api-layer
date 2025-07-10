/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas;

import org.springframework.context.ApplicationEvent;

public class ZaasServiceAvailableEvent extends ApplicationEvent {

    private static final long serialVersionUID = -9115429019604153949L;

    public ZaasServiceAvailableEvent(Object source) {
        super(source);
    }

}
