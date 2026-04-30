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

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ZaasContextClosedEventListener implements ApplicationListener<ContextClosedEvent> {

    private final ZaasStartupListener zaasStartupListener;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        zaasStartupListener.onContextClosed();
    }

}
