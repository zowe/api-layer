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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;

import java.io.UnsupportedEncodingException;

/**
 * Represents a record data structure in VSAM file.
 *
 * The structure is composed of {@link VsamKey} and {@link KeyValue}, serialized to JSON format.
 * Configuration is driven from {@link VsamConfig}
 *
 * Constructors provide ways to create record from raw bytes or from POJOs.
 * Provides methods to serialize to bytes in platform's encoding.
 */

public class VsamRecord {

    private final VsamConfig config;

    private String serviceId;

    private VsamKey key;
    private KeyValue keyValue;

    private ObjectMapper mapper = new ObjectMapper();

    private static final String UNSUPPORTED_ENCODING_MESSAGE = "Unsupported encoding: ";

    public VsamRecord(VsamConfig config, String serviceId, KeyValue kv) {
        this.config = config;
        this.serviceId = serviceId;
        this.keyValue = kv;
        this.key = new VsamKey(config);
    }

    public VsamRecord(VsamConfig config, byte[] recordData) throws VsamRecordException {
        this.config = config;
        this.key = new VsamKey(config);

        try {
            String recordString = new String(recordData, config.getEncoding());
            this.keyValue = mapper.readValue(recordString.substring(config.getKeyLength()).trim(), KeyValue.class);
            this.serviceId = keyValue.getServiceId();
        } catch (UnsupportedEncodingException e) {
            throw new VsamRecordException(UNSUPPORTED_ENCODING_MESSAGE + config.getEncoding(), e);
        } catch (JsonProcessingException e) {
            throw new VsamRecordException("Failure deserializing the record value to KeyValue object", e);
        }

    }

    public byte[] getBytes() throws VsamRecordException {
        try {
            byte[] bytes = StringUtils.rightPad(key.getKey(serviceId, keyValue.getKey()) + mapper.writeValueAsString(keyValue), config.getRecordLength())
                .getBytes(config.getEncoding());
            if (bytes.length > config.getRecordLength()) {
                throw new StorageException(Messages.PAYLOAD_TOO_LARGE.getKey(), Messages.PAYLOAD_TOO_LARGE.getStatus(), keyValue.getKey());
            }

            return bytes;


        } catch (UnsupportedEncodingException e) {
            throw new VsamRecordException(UNSUPPORTED_ENCODING_MESSAGE + config.getEncoding(), e);
        } catch (JsonProcessingException e) {
            throw new VsamRecordException("Failure serializing KeyValue object to Json: " + config.getEncoding(), e);
        }
    }

    public byte[] getKeyBytes() throws VsamRecordException {
        try {
            return key.getKeyBytes(serviceId, keyValue);
        } catch (UnsupportedEncodingException e) {
            throw new VsamRecordException(UNSUPPORTED_ENCODING_MESSAGE + config.getEncoding(), e);
        }
    }

    public String getServiceId() {
        return serviceId;
    }

    public KeyValue getKeyValue() {
        return keyValue;
    }

    @Override
    public String toString() {
        return "VsamRecord{" +
            "config=" + config +
            ", serviceId='" + serviceId + '\'' +
            ", key=" + key.getKey(serviceId, keyValue.getKey()) +
            ", keyValue=" + keyValue +
            '}';
    }
}
