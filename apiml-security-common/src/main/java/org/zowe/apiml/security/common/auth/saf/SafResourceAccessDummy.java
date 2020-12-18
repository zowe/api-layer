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

public class SafResourceAccessDummy implements SafResourceAccessVerifying {

    private static final String SAF_ACCESS = "safAccess";
    private static final String DEFAULT_FILE_LOCATION = "saf.yml";
    private static final String DEFAULT_RESOURCE_LOCATION = "mock-saf.yml";

    private Map<ResourceUser, AccessLevel> resourceUserToAccessLevel = new HashMap<>();

    public SafResourceAccessDummy() throws IOException {
        File file = getFile();
        if (file.exists()) {
            try (
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis)
            ) {
                load(bis);
            }
        } else {
            try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_RESOURCE_LOCATION)) {
                load(inputStream);
            }
        }
    }

    protected File getFile() {
        return new File(DEFAULT_FILE_LOCATION);
    }

    public SafResourceAccessDummy(InputStream inputStream) {
        load(inputStream);
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

    private void load(InputStream inputStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(inputStream);
        load(data);
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

    private void load(Map<String, Object> data) {
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
