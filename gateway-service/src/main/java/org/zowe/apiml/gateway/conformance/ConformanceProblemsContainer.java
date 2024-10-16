/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.text.StrSubstitutor;
import org.zowe.apiml.message.api.ApiMessage;

import java.util.*;


/**
 * Java class that is used to keep track of found conformance issues
 */
@EqualsAndHashCode(callSuper = true)
public class ConformanceProblemsContainer extends HashMap<String, Set<String>> {

    private final String serviceId;
    private static final String RESPONSE_MESSAGE_TEMPLATE = """
        {
            "messageAction": "${messageAction}",
            "messageContent": {
                "The service ${serviceId} is not conformant": ${messageContent}
            },
            "messageKey": "${messageKey}",
            "messageNumber": "${messageNumber}",
            "messageReason": "${messageReason}",
            "messageType": "${messageType}"
        }
    """;

    ConformanceProblemsContainer(String serviceId) {
        super();
        this.serviceId = serviceId;
    }

    public void add(String key, List<String> values) {
        if (values == null) {
            return;
        }
        if (this.get(key) == null || this.get(key).isEmpty()) {
            super.put(key, new HashSet<>(values));
            return;
        }
        for (String value : values) {
            this.get(key).add(value);
        }
    }

    public void add(String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        this.add(key, new ArrayList<>(Collections.singleton(value)));
    }


    @Override
    public int size() {
        int result = 0;
        for (Set<String> value : this.values()) {
            if (value == null) {
                continue;
            }
            result += value.size();
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("{");
        boolean firstLoop = true;

        ArrayList<String> sortedKeySet = new ArrayList<>(this.keySet());
        sortedKeySet.remove(null);  // since it used to be a set this removes all nulls
        sortedKeySet.sort(null);
        for (String key : sortedKeySet) {

            if (this.get(key) == null || this.get(key).isEmpty()) {
                continue;
            }

            if (!firstLoop) {
                result.append(",");
            }

            result.append("\"").append(key).append("\"");
            result.append(":[");

            boolean firstInnerLoop = true;
            for (String i : get(key)) {
                if (!firstInnerLoop) {
                    result.append(",");
                }
                try {
                    result.append(new ObjectMapper().writeValueAsString(i));
                } catch (JsonProcessingException e) {
                    continue;
                }
                firstInnerLoop = false;
            }
            result.append("]");
            firstLoop = false;
        }
        result.append("}");
        return result.toString();
    }


    public String createBadRequestAPIResponseBody(String key, ApiMessage correspondingAPIMessage) {
        Map<String, String> valuesMap = new HashMap<>();

        valuesMap.put("messageKey", key);
        valuesMap.put("messageContent", this.toString());
        valuesMap.put("serviceId", serviceId);
        valuesMap.put("messageReason", correspondingAPIMessage.getMessageReason());
        valuesMap.put("messageNumber", correspondingAPIMessage.getMessageNumber());
        valuesMap.put("messageType", correspondingAPIMessage.getMessageType().toString());
        valuesMap.put("messageAction", correspondingAPIMessage.getMessageAction());

        return new StrSubstitutor(valuesMap).replace(RESPONSE_MESSAGE_TEMPLATE);
    }
}
