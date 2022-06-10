/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service;

import org.zowe.apiml.caching.model.KeyValue;

import java.util.Map;

/**
 * Every supported storage backend needs to have an implementation of the Storage.
 */
public interface Storage {
    /**
     * Store new KeyValue pair in the storage. If there is a key collision null is returned.
     *
     * @param serviceId Id of the service to store the value for
     * @param toCreate  KeyValue pair to be created.
     * @return The stored KeyValue pair or null.
     */
    KeyValue create(String serviceId, KeyValue toCreate);

    /**
     * Store new KeyValue pair in the storage. The entry will be stored in a list for a specific key.
     *
     * @param serviceId Id of the service to store the value for
     * @param toCreate  KeyValue pair to be created.
     */
    KeyValue storeListItem(String serviceId, KeyValue toCreate) throws StorageException;

    /**
     * Return all the items in the list for a specific key.
     *
     * @param serviceId Id of the service to load all key/value pairs
     * @param key       key to lookup
     * @return Map with the key/value pairs or null if there is none existing.
     */
    Map<String, String> getAllMapItems(String serviceId, String key) throws StorageException;

    /**
     * Returns the keys associated with the provided keys.
     *
     * @param serviceId Id of the service to read value for
     * @param key       key to lookup
     * @return KeyValue associated with the value
     */
    KeyValue read(String serviceId, String key);

    /**
     * Replaces the value for the given key with the new value. If there is no existing key/value pair null is returned.
     *
     * @param serviceId Id of the service to store the value for.
     * @param toUpdate  Value to store instead of the original one.
     * @return Updated key/value pair or null.
     */
    KeyValue update(String serviceId, KeyValue toUpdate);

    /**
     * Delete the key/value pair if it exists within the context of the service. If there is none existing null
     * is returned.
     *
     * @param serviceId Id of the service to delete the value for.
     * @param toDelete  Key to delete from the storage.
     * @return Deleted key/value pair or null.
     */
    KeyValue delete(String serviceId, String toDelete);

    /**
     * Return all the key/value pairs for given service id.
     *
     * @param serviceId Id of the service to load all key/value pairs
     * @return Map with the key/value pairs or null if there is none existing.
     */
    Map<String, KeyValue> readForService(String serviceId);

    /**
     * Delete all key value pairs.
     *
     * @param serviceId Id of the service to delete all key/value pairs for.
     */
    void deleteForService(String serviceId);
}
