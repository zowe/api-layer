/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.cloud.client.ServiceInstance;
import org.zowe.apiml.config.ApiInfo;

import java.io.StringWriter;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_DESCRIPTION;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_TITLE;

@Slf4j
public class SubstituteSwaggerGenerator {
    private final VelocityEngine ve = new VelocityEngine();

    public SubstituteSwaggerGenerator() {
        ve.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        ve.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
        ve.init();
    }

    public String generateSubstituteSwaggerForService(ServiceInstance serviceInstance,
                                                      ApiInfo api,
                                                      String gatewayScheme, String gatewayHost) {
        log.warn("Generating substitute swagger for serviceInstance '{}' API '{} {}'", serviceInstance.getInstanceId(), api.getApiId(), api.getVersion());
        String title = serviceInstance.getMetadata().get(SERVICE_TITLE);
        String description = serviceInstance.getMetadata().get(SERVICE_DESCRIPTION);
        String basePath = (api.getGatewayUrl().startsWith("/") ? "" : "/") + serviceInstance.getServiceId().toLowerCase()
            + (api.getGatewayUrl().endsWith("/") ? "" : "/") + api.getGatewayUrl();

        Template t = ve.getTemplate("substitute_swagger.json");
        VelocityContext context = new VelocityContext();
        context.put("title", title);
        context.put("description", description);
        context.put("version", api.getVersion());
        context.put("scheme", gatewayScheme);
        context.put("host", gatewayHost);
        context.put("basePath", basePath);
        context.put("documentationUrl", api.getDocumentationUrl());

        StringWriter w = new StringWriter();
        t.merge(context, w);

        return w.toString();
    }
}
