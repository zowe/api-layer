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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.extension.ExtensionDefinition.ApimlServices;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Slf4j
public class ExtensionConfigReader {

    private final ZoweRuntimeEnvironment environment;

    public ExtensionConfigReader(ZoweRuntimeEnvironment environment) {
        this.environment = environment;
    }

    public String[] getBasePackages() {
        return getEnabledExtensions()
            .stream()
            .map(ExtensionDefinition::getApimlServices)
            .filter(Objects::nonNull)
            .map(ApimlServices::getBasePackage)
            .filter(Objects::nonNull)
            .toList()
            .toArray(new String[0]);
    }

    private List<ExtensionDefinition> getEnabledExtensions() {
        List<String> installedComponents = environment.getInstalledComponents();
        List<String> enabledComponents = environment.getEnabledComponents();
        List<ExtensionDefinition> extensions = new ArrayList<>();

        for (String installedComponent : installedComponents) {
            if (enabledComponents.contains(installedComponent)) {
                try {
                    extensions.add(readComponentManifest(installedComponent));
                } catch (ExtensionManifestReadException e) {
                    log.error("Failed reading component {} manifest", installedComponent, e);
                }
            }
        }
        return extensions;
    }

    private ExtensionDefinition readComponentManifest(String installedComponent) {
        String parentPath = environment.getWorkspaceDirectory() + File.separator + installedComponent;
        Path manifestYamlPath = Paths.get(parentPath + File.separator + "manifest.yaml");
        Path manifestJsonPath = Paths.get(parentPath + File.separator + "manifest.json");

        Optional<ExtensionDefinition> definition = readComponentManifestWithCharset(Charset.defaultCharset(), manifestYamlPath, manifestJsonPath);
        if (definition.isPresent()) {
            return definition.get();
        } else {
            return readComponentManifestWithCharset(Charset.forName("IBM1047"), manifestYamlPath, manifestJsonPath)
                .orElseThrow(() -> new ExtensionManifestReadException("Could not read manifest in either " + Charset.defaultCharset() + " nor in IBM1047 encoding"));
        }
    }

    private Optional<ExtensionDefinition> readComponentManifestWithCharset(Charset charset, Path yamlPath, Path jsonPath) {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()).configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        ObjectMapper jsonMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            if (Files.exists(yamlPath)) {
                return Optional.ofNullable(yamlMapper.readValue(new String(Files.readAllBytes(yamlPath), charset), ExtensionDefinition.class));
            } else if (Files.exists(jsonPath)) {
                return Optional.ofNullable(jsonMapper.readValue(new String(Files.readAllBytes(jsonPath), charset), ExtensionDefinition.class));
            } else {
                log.debug("None of these files were found: {} nor {} ", yamlPath, jsonPath);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.debug("File {}/{} does not have charset {}: {}", yamlPath, jsonPath, charset, e.getMessage());
            return Optional.empty();
        }
    }
}
