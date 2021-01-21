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


@RequiredArgsConstructor
@Slf4j
public class RemoveOldestStrategy implements EvictionStrategy {
    private final VsamConfig vsamConfig;
    private VsamFile file;

    @Override
    public void evict(String key) {
        file = new VsamFile(vsamConfig, VsamConfig.VsamOptions.READ);
        removeOldestRecord();
    }

    public void removeOldestRecord() {
        VsamRecord oldest = null;
        try {
            byte[] recBuf = new byte[vsamConfig.getRecordLength()];
            int overflowProtection = 10000;
            while (file.getZfile().read(recBuf) != -1) {
                VsamRecord record = new VsamRecord(vsamConfig, "", recBuf);
                VsamRecord current = record;
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
        } catch (ZFileException | VsamRecordException e) {
            log.error(e.toString());
        }
        checkAndRemoveRecord(oldest);
    }

    private void checkAndRemoveRecord(VsamRecord oldest) {
        if (oldest != null) {
            try {
                file.getZfile().locate(oldest.getKeyBytes(),
                    ZFileConstants.LOCATE_KEY_EQ);
                file.getZfile().delrec();
            } catch (ZFileException | VsamRecordException e) {
                log.error(e.toString());
            }
        }
    }
}
