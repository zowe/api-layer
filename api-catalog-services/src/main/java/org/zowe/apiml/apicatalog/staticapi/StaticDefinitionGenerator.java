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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class StaticDefinitionGenerator {

    // TODO use the correct api-defs folder
    private String LOCATION = "./";

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    public StaticAPIResponse generateFile(String file) throws IOException {

        AtomicReference<String> fileName = new AtomicReference<>("");
        String serviceId = StringUtils.substringBetween(file, "serviceId: ", "\\n");
        fileName.set(LOCATION + serviceId + ".yml");
        file = file.replace("\\n", System.lineSeparator());
        file = file.substring(1, file.length() - 1);

        checkIfFileExists(serviceId);

        try(FileOutputStream fos = new FileOutputStream(fileName.get())) {
            fos.write(file.getBytes(StandardCharsets.UTF_8));
            fos.close();
            return new StaticAPIResponse(201, String.format("The static definition file has been created! Its location is: %s", LOCATION));
        } catch (IOException e) {
            apimlLog.log("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed",  e.getMessage());
            throw new IOException(e);
        }
    }

    private void checkIfFileExists(String serviceId) throws FileAlreadyExistsException {
        File outFile = new File(LOCATION + serviceId + ".yml");
        if (outFile.exists()) {
            throw new FileAlreadyExistsException(String.format("The static definition file %s already exists!", LOCATION));
        }
    }

}
