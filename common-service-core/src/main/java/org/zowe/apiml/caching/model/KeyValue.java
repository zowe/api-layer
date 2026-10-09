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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Data POJO that represents entry in caching service
 */
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
     * Requested time-to-live of the entry, in seconds.
     * <p>
     * Deliberately {@code transient} so the Java-serialized form of this class stays
     * byte-identical to the previous release.
     */
    @ToString.Exclude
    private transient Long ttlSeconds;

    /**
     * Creates the entry from its final fields. Used by Jackson, so the final fields are bound without
     * relying on {@code MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS}, which Jackson 3 disables by default.
     * A missing {@code key} or {@code value} stays {@code null} so the payload validation can reject it.
     *
     * @param key     the key of the entry
     * @param value   the value of the entry
     * @param created the creation timestamp in milliseconds, the current time when {@code null}
     */
    @JsonCreator
    public KeyValue(
        @JsonProperty("key") String key,
        @JsonProperty("value") String value,
        @JsonProperty("created") String created
    ) {
        this.key = key;
        this.value = value;
        this.serviceId = "";
        this.created = created == null ? currentTime() : created;
    }

    public KeyValue(String key, String value) {
        this(key, value, (String) null);
    }

    public KeyValue(String key, String value, Long ttlSeconds) {
        this(key, value);
        this.ttlSeconds = ttlSeconds;
    }

    private static String currentTime() {
        return String.valueOf(new Date().getTime());
    }
}
