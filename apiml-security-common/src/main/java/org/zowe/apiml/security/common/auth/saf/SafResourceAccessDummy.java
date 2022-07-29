/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import lombok.Builder;
import lombok.Value;
import org.springframework.security.core.Authentication;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a dummy implementation of SAF resource checking. Combination of SAF resources and users who
 * has allowed a resource is defined in a file.
 * As a default is loaded file `saf.yml` from root folder. It is helpful for tests on local machine. If this file does
 * not exist it will load file `mock-saf.yml` from classpath. It can be use for unit test. Classpath of test override
 * the file from applications classpath. It is highly recommended to locate empty file `mock-saf` in each application
 * using this feature to allow start application without any other action.
 */
public class SafResourceAccessDummy implements SafResourceAccessVerifying {

    private static final String SAF_ACCESS = "safAccess";
    // Issue: it will read gateway/saf.yml even for the catalog if it exists
    // List of authorization files in the order according to the priority
    // Required for IDE (IntelliJ) which sets the root project directory as home dir
    private static final String[] DEFAULT_FILE_LOCATIONS = {
            "saf.yml",
            "gateway-service/saf.yml",
            "discovery-service/saf.yml",
            "api-catalog-services/saf.yml"
    };
    private static final String DEFAULT_RESOURCE_LOCATION = "mock-saf.yml";

    private Map<ResourceUser, AccessLevel> resourceUserToAccessLevel = new HashMap<>();

    public SafResourceAccessDummy() throws IOException {
        File file = getFile();
        if (file != null) {
            try (
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis)
            ) {
                loadDefinition(bis);
            }
        } else {
            try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_RESOURCE_LOCATION)) {
                loadDefinition(inputStream);
            }
        }
    }

    protected File getFile() {
        for (String fileName : DEFAULT_FILE_LOCATIONS) {
            File file = new File(fileName);
            if (file.exists()) return file;
        }

        return null;
    }

    public SafResourceAccessDummy(InputStream inputStream) {
        loadDefinition(inputStream);
    }

    @Override
    public boolean hasSafResourceAccess(Authentication authentication, String resourceClass, String resourceName, String accessLevel) {
        ResourceUser resourceUser = ResourceUser.builder()
            .resourceClass(resourceClass)
            .resourceName(resourceName)
            .userId(authentication.getName())
            .build();
        AccessLevel currentLevel = resourceUserToAccessLevel.get(resourceUser);
        if (currentLevel == null) return false;
        return currentLevel.compareTo(AccessLevel.valueOf(accessLevel)) >= 0;
    }

    /**
     * This method loads a YML file with description of allowed SAF resource.
     *
     * Structure of file:
     *
     * ```
     * safAccess:
     *  {CLASS}:
     *   {RESOURCE}:
     *    - {User ID}
     * ```
     * notes:
     *  - Classes and resources are mapped into a map, user IDs into list.
     *  - load method does not support formatting with dot, like {CLASS}.{RESOURCE}, each element has to be separated
     *  - safAccess is not required to define empty file
     *  - classes and resources cannot be defined without user ID list
     *  - method could be called multiple times, definition are added to current (loaded)
     *  - when a user has multiple definition of same class and resource, just the highest access level is loaded
     *
     * @param inputStream stream to be loaded
     */
    private void loadDefinition(InputStream inputStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);
        loadDefinition(data);
    }

    private void set(ResourceUser resourceUser, AccessLevel accessLevel) {
        AccessLevel currentLevel = resourceUserToAccessLevel.get(resourceUser);
        if ((currentLevel == null) || (currentLevel.compareTo(accessLevel) < 0)) {
            currentLevel = accessLevel;
        }
        resourceUserToAccessLevel.put(resourceUser, currentLevel);
    }

    private void set(String resourceClass, String resourceName, List<String> users, String levelName) {
        AccessLevel accessLevel = AccessLevel.valueOf(levelName);

        for (String userId : users) {
            ResourceUser resourceUser = ResourceUser.builder()
                .resourceClass(resourceClass)
                .resourceName(resourceName)
                .userId(userId)
                .build();
            set(resourceUser, accessLevel);
        }
    }

    private <T> T getSafAccess(Map<String, Object> data) {
        if (data == null) return null;
        return (T) data.get(SAF_ACCESS);
    }

    private void loadDefinition(Map<String, Object> data) {
        Map<String, Map<String, Map<String, List<String>>>> classes = getSafAccess(data);
        if (classes == null) return;

        for (Map.Entry<String, Map<String, Map<String, List<String>>>> clazz :  classes.entrySet()) {
            String resourceClass = clazz.getKey();
            for (Map.Entry<String, Map<String, List<String>>> resource : clazz.getValue().entrySet()) {
                String resourceName = resource.getKey();
                for (Map.Entry<String, List<String>> level : resource.getValue().entrySet()) {
                    String levelName = level.getKey().toUpperCase();
                    List<String> users = level.getValue();
                    set(resourceClass, resourceName, users, levelName);
                }
            }
        }
    }

    @Value
    @Builder
    private static class ResourceUser {

        String resourceClass;
        String resourceName;
        String userId;

    }

}
