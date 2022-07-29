/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.zfile.ZFile;
import org.zowe.apiml.zfile.ZFileConstants;
import org.zowe.apiml.zfile.ZFileException;

/**
 * This performs the warmup of VSAM during startup with retry
 * It has to be externalized from the VsamStorage because of @Retryable limitations
 * This is needed for service to start reliably when the VSAM file is being used
 * by another process at the moment.
 */
@Service
@Slf4j
public class VsamInitializer {

    @Retryable(value = UnsupportedOperationException.class, maxAttempts = 10)
    public void storageWarmup(VsamConfig config, ApimlLogger apimlLogger) {
        try (VsamFile file = new VsamFile(config, VsamConfig.VsamOptions.WRITE, true, apimlLogger)) {
            log.info("Vsam file open successful");
        }
    }

    /**
     * This method writes a record to file and deletes it immediately.
     * Use this method on freshly created empty VSAM to write the fist record
     * and to verify that records can be written.
     * <p>
     * Exceptions are thrown to give chance to the caller to react.
     *
     * @throws ZFileException
     * @throws VsamRecordException
     */
    public void warmUpVsamFile(ZFile zFile, VsamConfig vsamConfig) throws ZFileException, VsamRecordException {
        log.info("Warming up the vsam file by writing and deleting a record");
        log.info("VSAM file being used: {}", zFile.getActualFilename());

        VsamRecord vsamRec = new VsamRecord(vsamConfig, "delete", new KeyValue("me", "novalue"));

        log.info("Writing Record: {}", vsamRec);
        zFile.write(vsamRec.getBytes());

        boolean found = zFile.locate(vsamRec.getKeyBytes(), ZFileConstants.LOCATE_KEY_EQ);

        log.info("Test record for deletion found: {}", found);
        if (found) {
            byte[] recBuf = new byte[vsamConfig.getRecordLength()];
            zFile.read(recBuf); //has to be read before update/delete
            zFile.delrec();
            log.info("Test record deleted.");
        }
    }
}
