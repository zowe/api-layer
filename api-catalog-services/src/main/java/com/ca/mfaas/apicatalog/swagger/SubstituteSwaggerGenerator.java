/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger;

import com.ca.mfaas.product.model.ApiInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.PortType;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.http.ResponseEntity;

import java.io.StringWriter;

public class SubstituteSwaggerGenerator {
    private VelocityEngine ve = new VelocityEngine();

    public SubstituteSwaggerGenerator() {
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
    }

    public ResponseEntity<String> generateSubstituteSwaggerForService(InstanceInfo gateway, InstanceInfo service,
            ApiInfo api) {
        String title = service.getMetadata().get("mfaas.discovery.service.title");
        String description = service.getMetadata().get("mfaas.discovery.service.description");
        String scheme = gateway.isPortEnabled(PortType.SECURE) ? "https" : "http";

        String host = gateway.getHostName();
        if (scheme.equals("http")) {
            if (gateway.getPort() != 80) {
                host += ":" + gateway.getPort();
            }
        } else {
            if (gateway.getPort() != 443) {
                host += ":" + gateway.getSecurePort();
            }
        }

        String basePath = (api.getGatewayUrl().startsWith("/") ? "" : "/") + api.getGatewayUrl()
                + (api.getGatewayUrl().endsWith("/") ? "" : "/") + service.getAppName().toLowerCase();

        Template t = ve.getTemplate("substitute_swagger.json");
        VelocityContext context = new VelocityContext();
        context.put("title", title);
        context.put("description", description);
        context.put("version", api.getVersion());
        context.put("scheme", scheme);
        context.put("host", host);
        context.put("basePath", basePath);
        context.put("documentationUrl", api.getDocumentationUrl());

        StringWriter w = new StringWriter();
        t.merge(context, w);

        return ResponseEntity.ok(w.toString());
    }
}
