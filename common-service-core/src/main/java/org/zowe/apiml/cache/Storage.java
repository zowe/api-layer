/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cache;

import org.zowe.apiml.caching.model.KeyValue;

import java.util.Collection;
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
     * Store new KeyValue pair in the storage. The entry will be stored in a map under a specific map key.
     *
     * @param serviceId Id of the service to store the value for
     * @param mapKey key of the specific map underneath the key-value pair should be stored
     * @param toCreate  KeyValue pair to be created.
     */
    KeyValue storeMapItem(String serviceId, String mapKey, KeyValue toCreate) throws StorageException;

    /**
     * Return all the items in the specific map for a specific service.
     *
     * @param serviceId Id of the service to load all key/value pairs
     * @param mapKey key of the specific map to return
     * @return Map with the key/value pairs or null if there is none existing.
     */
    Map<String, String> getAllMapItems(String serviceId, String mapKey) throws StorageException;

    /**
     * Return all the items in all the maps for specific service
     *
     * @param serviceId Id of the service to load all key/value pairs
     * @return Map of all lists with the key/value pairs or null if there is none existing.
     */
    Map<String, Map<String, String>> getAllMaps(String serviceId) throws StorageException;

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

    /**
     * Delete a key/value pair from the rules map
     * @param serviceId the id of the service to identify the correct map
     * @param mapKey the map key
     */
    void removeNonRelevantRules(String serviceId, String mapKey);

    /**
     * Delete a key/value pair from the invalid tokens map
     * @param serviceId the id of the service to identify the correct map
     * @param mapKey the map key
     */
    void removeNonRelevantTokens(String serviceId, String mapKey);

    /**
     * Point lookup of a specific set of items spread over several maps, in one call.
     * <p>
     * This is the hot-path read for personal access token validation: it replaces downloading every map in
     * full with a handful of {@code get}s. Only the entries that exist are returned - a map key with no
     * matching item is omitted from the result entirely, and an absent record means "not present", never an
     * error. Any transport or storage failure must propagate, so that the caller can fail closed.
     *
     * @param serviceId     Id of the service the items belong to
     * @param keysByMapKey  the item keys to look up, grouped by the map they live in
     * @return the found entries, grouped by map key; never null
     */
    Map<String, Map<String, String>> getMapItems(String serviceId, Map<String, Collection<String>> keysByMapKey) throws StorageException;

    /**
     * Read the pre-cutover, whole-map revocation layout.
     * <p>
     * Exists purely so that personal access tokens issued before the per-item store was introduced keep being
     * checked against revocations made before it. It is never written to, and the last caller disappears once
     * every pre-cutover token has expired - at which point this method, its implementation, its endpoint and
     * its client method are all deleted together.
     *
     * @param serviceId Id of the service to load the legacy maps for
     * @return Map of all legacy maps with their key/value pairs; never null
     * @deprecated superseded by the per-item layout; scheduled for removal with the legacy read path.
     */
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    Map<String, Map<String, String>> getAllLegacyMaps(String serviceId) throws StorageException;
}
