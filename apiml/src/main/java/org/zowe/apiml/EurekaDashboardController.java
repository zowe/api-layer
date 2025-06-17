/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zowe.apiml;

import com.netflix.appinfo.ApplicationInfoManager;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.EurekaController;
import org.springframework.cloud.netflix.eureka.server.EurekaProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Primary;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.result.view.RequestContext;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;

@Primary
@RestController
@RequestMapping("${eureka.dashboard.path:/eureka}")
public class EurekaDashboardController {

    private final EurekaController original;

    private final MessageSource messageSource;

    private final TemplateProcessor templateProcessor;

    private final Template statusTemplate;
    private final Template lastnTemplate;

    @Autowired
    public EurekaDashboardController(
        ApplicationInfoManager applicationInfoManager,
        EurekaProperties eurekaProperties,
        FreeMarkerConfigurer freeMarkerConfigurer,
        MessageSource messageSource) throws IOException {
        this(new EurekaController(applicationInfoManager, eurekaProperties), freeMarkerConfigurer, messageSource, new TemplateProcessor());
    }

    EurekaDashboardController(
        EurekaController original,
        FreeMarkerConfigurer freeMarkerConfigurer,
        MessageSource messageSource,
        TemplateProcessor processor) throws IOException {
        this.original = original;
        this.messageSource = messageSource;
        this.templateProcessor = processor;
        var configuration = freeMarkerConfigurer.getConfiguration();
        this.statusTemplate = configuration.getTemplate("eureka/status.ftlh", null, null, null, true, true);
        this.lastnTemplate = configuration.getTemplate("eureka/lastn.ftlh", null, null, null, true, true);
    }

    @GetMapping
    public Mono<String> status(
        ServerWebExchange serverWebExchange,
        Map<String, Object> model
    ) throws TemplateException, IOException {
        original.status(null, model);
        model.put("springMacroRequestContext", new RequestContext(serverWebExchange, model, messageSource));
        return Mono.just(templateProcessor.process(statusTemplate, model));
    }

    @GetMapping("/lastn")
    public Mono<String> lastn(
        ServerWebExchange serverWebExchange,
        Map<String, Object> model
    ) throws TemplateException, IOException {
        original.status(null, model);
        model.put("springMacroRequestContext", new RequestContext(serverWebExchange, model, messageSource));
        return Mono.just(templateProcessor.process(lastnTemplate, model));
    }

    static class TemplateProcessor {

        String process(Template template, Map<String, Object> model) throws IOException, TemplateException {
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        }

    }

}
