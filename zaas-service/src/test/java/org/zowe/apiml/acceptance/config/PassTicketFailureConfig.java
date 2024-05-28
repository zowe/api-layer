/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.config;

import com.netflix.zuul.ZuulFilter;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.zaas.security.service.PassTicketException;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * Configuration that creates a Zuul filter which throws a PassTicketException to test the error handler.
 */
public class PassTicketFailureConfig {

    @Bean
    public ZuulFilter zuulFilter() {
        return new ZuulFilter() {
            @Override
            public String filterType() {
                return PRE_TYPE;
            }

            @Override
            public int filterOrder() {
                return 0;
            }

            @Override
            public boolean shouldFilter() {
                return true;
            }

            @Override
            public Object run() {
                throw new PassTicketException("problem");
            }
        };
    }
}
