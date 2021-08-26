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

import lombok.extern.slf4j.Slf4j;
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

/**
 * Service to handle the creation of the static definition file
 * Allows the generation and the override of the .yml
 */
@Service
@Slf4j
public class StaticDefinitionGenerator {

    // TODO use the correct api-defs folder
    private String LOCATION = "./";

    AtomicReference<String> fileName = new AtomicReference<>("");

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    public StaticAPIResponse generateFile(String file) throws IOException {

        String serviceId = StringUtils.substringBetween(file, "serviceId: ", "\\n");
        file = formatFile(file);
        fileName.set(LOCATION + serviceId + ".yml");

        checkIfFileExists(serviceId);
        String message = "The static definition file has been created by the user! Its location is: %s";
        return writeFileAndSendResponse(file, fileName, String.format(message, fileName));
    }

    public StaticAPIResponse overrideFile(String file) throws IOException {
        String serviceId = StringUtils.substringBetween(file, "serviceId: ", "\\n");
        file = formatFile(file);
        fileName.set(LOCATION + serviceId + ".yml");
        String message = "The static definition file %s has been overwritten by the user!";
        return writeFileAndSendResponse(file, fileName, String.format(message, fileName));
    }

    private String formatFile(String file) {
        file = file.replace("\\n", System.lineSeparator());
        file = file.substring(1, file.length() - 1);
        return file;
    }

    private StaticAPIResponse writeFileAndSendResponse(String file, AtomicReference<String> fileName, String message) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName.get())) {
            fos.write(file.getBytes(StandardCharsets.UTF_8));
            fos.close();

            log.debug(message);
            return new StaticAPIResponse(201, message);
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
