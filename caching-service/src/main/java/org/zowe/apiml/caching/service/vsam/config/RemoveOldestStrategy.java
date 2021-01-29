/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.caching.service.EvictionStrategy;
import org.zowe.apiml.caching.service.vsam.VsamFile;
import org.zowe.apiml.caching.service.vsam.VsamRecord;
import org.zowe.apiml.caching.service.vsam.VsamRecordException;
import org.zowe.apiml.zfile.ZFileConstants;
import org.zowe.apiml.zfile.ZFileException;

import java.io.UnsupportedEncodingException;
import java.util.Optional;


@RequiredArgsConstructor
@Slf4j
public class RemoveOldestStrategy implements EvictionStrategy {
    private final VsamConfig vsamConfig;

    private final VsamFile file;

    @Override
    public void evict(String key) {
        removeOldestRecord();
    }

    public void removeOldestRecord() {
        log.info("Inside removeOldestRecord.");
        VsamRecord oldest = null;
        try {
            byte[] ignoreKey = " ".getBytes(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
            byte[] recBuf = new byte[vsamConfig.getRecordLength()];
            Optional<byte[]> readRecord;
            int overflowProtection = 10000;
            file.getZfile().locate(ignoreKey,
                ZFileConstants.LOCATE_KEY_FIRST);
            while ((readRecord = file.readBytes(recBuf)).isPresent()) {
                VsamRecord current = new VsamRecord(vsamConfig, readRecord.get());
                if (oldest == null) {
                    oldest = current;
                }
                long oldestCreated = Long.parseLong(oldest.getKeyValue().getCreated());
                long currentCreated = Long.parseLong(current.getKeyValue().getCreated());

                if (oldestCreated > currentCreated) {
                    oldest = current;
                }
                overflowProtection--;
                if (overflowProtection <= 0) {
                    log.error("Maximum number of records retrieved, stopping the retrieval");
                    break;
                }
            }
        } catch (ZFileException | VsamRecordException | UnsupportedEncodingException e) {
            log.error(e.toString());
        }
        checkAndRemoveRecord(oldest);
    }

    private void checkAndRemoveRecord(VsamRecord oldest) {
        if (oldest != null) {
            log.info("Record to remove {}", oldest);
            log.info("Removing the oldest record {}", oldest.getKeyValue().getKey());
            Optional<VsamRecord> returned = file.delete(oldest);
            if (returned.isPresent()) {
                log.info("The oldest record has been successfully removed!");
            }
        }
        log.info("It was not possible to delete the record.");
    }
}
