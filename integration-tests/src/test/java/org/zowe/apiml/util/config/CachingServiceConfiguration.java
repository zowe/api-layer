/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.config;

import lombok.*;
import org.zowe.apiml.product.constants.CoreService;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CachingServiceConfiguration extends ServiceConfiguration {

    @Override
    public String getServiceId() {
        return CoreService.CACHING.getServiceId();
    }

    @Override
    public String getServletContext() {
        return "/" + getServiceId() + "/";
    }

    @Override
    public boolean isBasicAuthenticationSupported() {
        return false;
    }
}
