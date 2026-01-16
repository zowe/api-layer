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
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

import javax.annotation.Nonnull;

public abstract class ApimlOpenTelemetryResourceProvider implements ResourceProvider {

    abstract @Nonnull Attributes calculateAttributes();

    @Override
    public Resource createResource(@Nonnull ConfigProperties config) {
        var attributesBuilder = Attributes.builder();

        attributesBuilder.putAll(calculateAttributes());
        return Resource.create(attributesBuilder.build());
    }

}
