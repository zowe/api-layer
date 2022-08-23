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
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.EvictionStrategy;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.log.ApimlLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Class handles requests from controller and orchestrates operations on the low level VSAM File class
 */
@Slf4j
public class VsamStorage implements Storage {

    private VsamConfig vsamConfig;
    private EvictionStrategyProducer evictionStrategyProducer;
    private VsamFileProducer producer = new VsamFileProducer();
    private ApimlLogger apimlLog;

    public VsamStorage(VsamConfig vsamConfig, VsamInitializer vsamInitializer, ApimlLogger apimlLog, EvictionStrategyProducer evictionStrategyProducer) {
        log.info("Using VSAM storage for the cached data");

        this.apimlLog = apimlLog;

        String vsamFileName = vsamConfig.getFileName();
        if (vsamFileName == null || vsamFileName.isEmpty()) {
            apimlLog.log("org.zowe.apiml.cache.errorInitializingStorage", "vsam", "wrong Configuration", "VSAM Filename must be valid");

            throw new IllegalArgumentException("Vsam filename must be valid");
        }

        this.vsamConfig = vsamConfig;
        this.evictionStrategyProducer = evictionStrategyProducer;

        log.info("Using Vsam configuration: {}", vsamConfig);
        vsamInitializer.storageWarmup(vsamConfig, apimlLog);
    }

    public VsamStorage(VsamConfig vsamConfig, VsamInitializer vsamInitializer, VsamFileProducer producer, ApimlLogger apimlLogger, EvictionStrategyProducer evictionStrategyProducer) {
        this(vsamConfig, vsamInitializer, apimlLogger, evictionStrategyProducer);

        this.producer = producer;
    }

    private EvictionStrategy provideStrategy(VsamFile file) {
        return evictionStrategyProducer.evictionStrategy(file);
    }

    @Override
    @Retryable(value = {RetryableVsamException.class, IllegalStateException.class, UnsupportedOperationException.class})
    public KeyValue create(String serviceId, KeyValue toCreate) {
        log.info("Writing record: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());
        KeyValue result = null;

        try (VsamFile file = producer.newVsamFile(vsamConfig, VsamConfig.VsamOptions.WRITE, apimlLog)) {
            toCreate.setServiceId(serviceId);
            VsamRecord vsamRec = new VsamRecord(vsamConfig, serviceId, toCreate);
            int currentSize = file.countAllRecords();
            log.info("Current Size {}.", currentSize);

            if (aboveThreshold(currentSize)) {
                EvictionStrategy strategy = provideStrategy(file);
                log.info("Evicting record using the {} strategy", vsamConfig.getGeneralConfig().getEvictionStrategy());
                strategy.evict(toCreate.getKey());
            }
            Optional<VsamRecord> returned = file.create(vsamRec);
            if (returned.isPresent()) {
                result = returned.get().getKeyValue();
            }
        }

        if (result == null) {
            throw new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), toCreate.getKey(), serviceId);
        }

        return result;
    }

    @Override
    public KeyValue storeMapItem(String serviceId, String mapKey, KeyValue toCreate) throws StorageException {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    @Override
    public Map<String, String> getAllMapItems(String serviceId, String mapKey) throws StorageException {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    @Override
    public Map<String, Map<String, String>> getAllMaps(String serviceId) throws StorageException {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    private boolean aboveThreshold(int currentSize) {
        return currentSize >= vsamConfig.getGeneralConfig().getMaxDataSize();
    }

    @Override
    @Retryable(value = {RetryableVsamException.class})
    public KeyValue read(String serviceId, String key) {
        log.info("Reading Record: {}|{}|{}", serviceId, key, "-");
        KeyValue result = null;

        try (VsamFile file = producer.newVsamFile(vsamConfig, VsamConfig.VsamOptions.READ, apimlLog)) {

            VsamRecord vsamRec = new VsamRecord(vsamConfig, serviceId, new KeyValue(key, "", serviceId));

            Optional<VsamRecord> returned = file.read(vsamRec);
            if (returned.isPresent()) {
                result = returned.get().getKeyValue();
            }
        }

        if (result == null) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), key, serviceId);
        }

        return result;
    }

    @Override
    @Retryable(value = {RetryableVsamException.class, IllegalStateException.class, UnsupportedOperationException.class})
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        log.info("Updating Record: {}|{}|{}", serviceId, toUpdate.getKey(), toUpdate.getValue());
        KeyValue result = null;

        try (VsamFile file = producer.newVsamFile(vsamConfig, VsamConfig.VsamOptions.WRITE, apimlLog)) {
            toUpdate.setServiceId(serviceId);
            VsamRecord vsamRec = new VsamRecord(vsamConfig, serviceId, toUpdate);

            Optional<VsamRecord> returned = file.update(vsamRec);
            if (returned.isPresent()) {
                result = returned.get().getKeyValue();
            }
        }

        if (result == null) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toUpdate.getKey(), serviceId);
        }

        return result;
    }

    @Override
    @Retryable(value = {RetryableVsamException.class, IllegalStateException.class, UnsupportedOperationException.class})
    public KeyValue delete(String serviceId, String toDelete) {

        log.info("Deleting Record: {}|{}|{}", serviceId, toDelete, "-");
        KeyValue result = null;

        try (VsamFile file = producer.newVsamFile(vsamConfig, VsamConfig.VsamOptions.WRITE, apimlLog)) {

            VsamRecord vsamRec = new VsamRecord(vsamConfig, serviceId, new KeyValue(toDelete, "", serviceId));

            Optional<VsamRecord> returned = file.delete(vsamRec);
            if (returned.isPresent()) {
                result = returned.get().getKeyValue();
            }
        }

        if (result == null) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toDelete, serviceId);
        }

        return result;
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {

        log.info("Reading All Records: {}|{}|{}", serviceId, "-", "-");
        Map<String, KeyValue> result = new HashMap<>();
        List<VsamRecord> returned;

        try (VsamFile file = producer.newVsamFile(vsamConfig, VsamConfig.VsamOptions.READ, apimlLog)) {
            returned = file.readForService(serviceId);
        }

        returned.forEach(vsamRecord -> result.put(vsamRecord.getKeyValue().getKey(), vsamRecord.getKeyValue()));

        return result;
    }

    @Override
    public void deleteForService(String serviceId) {
        log.info("Deleting All Records: {}|{}|{}", serviceId, "-", "-");

        try (VsamFile file = producer.newVsamFile(vsamConfig, VsamConfig.VsamOptions.WRITE, apimlLog)) {
            file.deleteForService(serviceId);
        }
    }

    @Override
    public void removeNonRelevantTokens(String serviceId, String mapKey) {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    @Override
    public void removeNonRelevantRules(String serviceId, String mapKey) {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }
}
