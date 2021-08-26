/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.staticapi;

import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StaticDefinitionGenerator {
    private String LOCATION = "./";

    public void generateFile(String file) throws IOException {
        // TODO read service id
        AtomicReference<String> fileName = new AtomicReference<>("");
        Pattern pattern = Pattern.compile("serviceId:(.*?)\\n");
        Matcher matcher = pattern.matcher(file);
        while (matcher.find()) {
            System.out.println(matcher.group(1));
            fileName.set(matcher.group(1));
            fileName.set(LOCATION + matcher.group(1) + ".yml");

        }
        System.out.println(file);


        try(FileOutputStream fos = new FileOutputStream(String.valueOf(fileName))) {
            fos.write(file.getBytes(StandardCharsets.UTF_8));
        }

    }

}
