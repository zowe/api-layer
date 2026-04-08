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

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;

/**
 * This class allows to import the OpenTelemetryAutoConfiguration class if the otel.sdk.disabled property is not set to true.
 * The effect is the same as using the AutoConfiguration directly but allows control over the order of the beans creation.
 */
public class OpenTelemetryImportSelector implements ImportSelector, EnvironmentAware {

    private Environment environment;

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        var imports = new ArrayList<>();

        if (isOtelEnabled()) {
            imports.add("io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration");
        }
        return imports.toArray(new String[0]);
    }

    private boolean isOtelEnabled() {
        if (environment == null) {
            return false;
        }

        return !Boolean.parseBoolean(environment.getProperty("otel.sdk.disabled", "true"));
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
