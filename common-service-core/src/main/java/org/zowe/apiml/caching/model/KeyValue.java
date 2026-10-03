/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Data POJO that represents entry in caching service
 */
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class KeyValue implements Serializable {

    @Serial
    private static final long serialVersionUID = 4831101523346346817L;
    private final String key;
    private final String value;
    private String serviceId;
    private final String created;

    /**
     * Requested time-to-live of the entry, in seconds. Only honoured by storage backends with native
     * expiration support (Infinispan); ignored elsewhere. {@code null} means "no TTL requested" and lets
     * the storage derive one, or store the entry without expiration.
     * <p>
     * Deliberately {@code transient} so the Java-serialized form of this class - which is the value type of
     * the replicated and persisted {@code zoweCache}, holding among other things the PAT salt - stays
     * byte-identical to the previous release. Jackson still emits the property, because
     * {@code MapperFeature.PROPAGATE_TRANSIENT_MARKER} is off by default and Lombok generates a getter;
     * {@code @JsonInclude(NON_EMPTY)} keeps it off the wire while unset. Excluded from {@code toString()} so
     * pre-existing log lines that print a whole {@code KeyValue} do not silently change shape;
     * {@code equals()}/{@code hashCode()} exclude transient fields by construction in Lombok.
     */
    @ToString.Exclude
    private transient Long ttlSeconds;

    public KeyValue(String key, String value) {
        this.key = key;
        this.value = value;
        this.serviceId = "";
        this.created = currentTime();
    }

    public KeyValue(String key, String value, Long ttlSeconds) {
        this(key, value);
        this.ttlSeconds = ttlSeconds;
    }

    private static String currentTime() {
        return String.valueOf(new Date().getTime());
    }

    @JsonCreator
    public KeyValue() {
        key = "";
        value = "";
        serviceId = "";
        created = currentTime();
    }
}
