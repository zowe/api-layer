/*
* This program and the accompanying materials are made available under the terms of the
* Eclipse Public License v2.0 which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Copyright Contributors to the Zowe Project.
*/
package org.zowe.apiml.extension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.zowe.apiml.extension.ExtensionDefinition.ApimlServices;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExtensionConfigReader {

    private final ZoweRuntimeEnvironment environment;

    public ExtensionConfigReader(ZoweRuntimeEnvironment environment) {
        super();
        this.environment = environment;
    }

    public String[] getBasePackages() {
        return getEnabledExtensions()
            .stream()
            .map(ExtensionDefinition::getApimlServices)
            .filter(Objects::nonNull)
            .map(ApimlServices::getBasePackage)
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
            .toArray(new String[0]);
    }

    private List<ExtensionDefinition> getEnabledExtensions() {
        List<String> installedComponents = environment.getInstalledComponents();
        List<String> enabledComponents = environment.getEnabledComponents();
        List<ExtensionDefinition> extensions = new ArrayList<>();
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ObjectMapper jsonMapper = new ObjectMapper();

        for (String installedComponent : installedComponents) {
            if (enabledComponents.contains(installedComponent)) {
                String parentPath = environment.getExtensionDirecotry() + "/" + installedComponent;
                Path manifestYamlPath = Paths.get(parentPath + "/manifest.yaml");
                Path manifestJsonPath = Paths.get(parentPath + "/manifest.json");
                try {
                    if (Files.exists(manifestYamlPath)) {
                        extensions.add(yamlMapper.readValue(Files.readAllBytes(manifestYamlPath), ExtensionDefinition.class));
                    } else if (Files.exists(manifestJsonPath)) {
                        extensions.add(jsonMapper.readValue(Files.readAllBytes(manifestJsonPath), ExtensionDefinition.class));
                    } else {
                        log.info("No manifest found for component " + installedComponent);
                    }
                } catch (Exception e) {
                    log.error("Failed reading component manifests", e);
                }
            }
        }
        return extensions;
    }
}
