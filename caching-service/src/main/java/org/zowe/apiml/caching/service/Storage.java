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

import java.util.Collection;

/**
 * Every supported storage backend needs to have an implementation of the Storage.
 */
public interface Storage {
    KeyValue create(KeyValue toCreate);

    KeyValue readSpecific(String key);

    Collection<KeyValue> readForService(String serviceId);

    KeyValue update(KeyValue toUpdate);

    KeyValue delete(String key);
}
