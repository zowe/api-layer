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
import org.zowe.apiml.caching.config.VsamConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.util.ObjectUtil;

import java.util.*;

/**
 * Class handles requests from controller and orchestrates operations on the low level VSAM File class
 */

@Slf4j
public class VsamStorage implements Storage {

    VsamConfig vsamConfig;

    public VsamStorage(VsamConfig config) {

        log.info("Using VSAM storage for the cached data");

        ObjectUtil.requireNotNull(config.getFileName(), "Vsam filename cannot be null"); //TODO bean validation
        ObjectUtil.requireNotEmpty(config.getFileName(), "Vsam filename cannot be empty");

        this.vsamConfig = config;

        log.info("Using Vsam configuration: {}", vsamConfig);

        try (VsamFile file = new VsamFile(config, VsamConfig.VsamOptions.WRITE, true)) {
            log.info("Vsam file open successful");
        }

    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        log.info("Writing record: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());
        KeyValue result = null;

        try (VsamFile file = new VsamFile(vsamConfig, VsamConfig.VsamOptions.WRITE)) {

            VsamRecord record = new VsamRecord(vsamConfig, serviceId, toCreate);

            VsamRecord returned = file.create(record);
            result = returned.getKeyValue();
        }

        return result;
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        log.info("Reading Record: {}|{}|{}", serviceId, key, "-");
        VsamRecord result = null;

        try (VsamFile file = new VsamFile(vsamConfig, VsamConfig.VsamOptions.READ)) {

            VsamRecord record = new VsamRecord(vsamConfig, serviceId, new KeyValue(key, ""));

            result = file.read(record);
        }

        return result.getKeyValue(); //TODO Optional, nullpointer
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        log.info("Updating Record: {}|{}|{}", serviceId, toUpdate.getKey(), toUpdate.getValue());
        VsamRecord result = null;

        try (VsamFile file = new VsamFile(vsamConfig, VsamConfig.VsamOptions.WRITE)) {

            VsamRecord record = new VsamRecord(vsamConfig, serviceId, toUpdate);

            result = file.update(record);

        }

        return result.getKeyValue();
    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {

        log.info("Deleting Record: {}|{}|{}", serviceId, toDelete, "-");
        VsamRecord result = null;

        try (VsamFile file = new VsamFile(vsamConfig, VsamConfig.VsamOptions.WRITE)) {

            VsamRecord record = new VsamRecord(vsamConfig, serviceId, new KeyValue(toDelete, ""));

            result = file.delete(record);
        }

        return result.getKeyValue();
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {

        log.info("Reading All Records: {}|{}|{}", serviceId, "-", "-");
        Map<String, KeyValue> result = new HashMap<>();
        List<VsamRecord> returned = new ArrayList<>();

        try (VsamFile file = new VsamFile(vsamConfig, VsamConfig.VsamOptions.READ)) {
            returned = file.readForService(serviceId);
        }

        returned.forEach(vsamRecord -> result.put(vsamRecord.getKeyValue().getKey(), vsamRecord.getKeyValue()));

        return result;
    }

}
