/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

import io.opentelemetry.api.common.Attributes;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@ConditionalOnMissingBean(ApimlZosOpenTelemetryResourceProvider.class)
@Component
public class ApimlNonZosOpenTelemetryResourceProvider extends ApimlOpenTelemetryResourceProvider {

    @Override
    protected @Nonnull Attributes internalCalculateAttributes() {
        return Attributes.empty();
    }

    @Override
    protected String generateServiceName() {
        var systemName = StringUtils.isBlank(apimlId) ? hostname : apimlId;
        return systemName + ":" + port;
    }

}
