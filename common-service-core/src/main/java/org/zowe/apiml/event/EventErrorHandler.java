/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

@Slf4j
@Component
@ConditionalOnBean(SimpleApplicationEventMulticaster.class)
public class EventErrorHandler implements ErrorHandler {

    public EventErrorHandler(SimpleApplicationEventMulticaster multicaster) {
        multicaster.setErrorHandler(this);
    }

    @Override
    public void handleError(Throwable t) {
        log.error("Error occurred during processing an event", t);

        if (t instanceof RuntimeException re) {
            throw re;
        }
        throw new IllegalStateException(t);
    }

}
