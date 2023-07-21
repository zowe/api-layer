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
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import java.util.ArrayList;
import java.util.HashMap;




/**
 * Java class that is used to keep track of found conformance issues
 */
public class ConformanceProblemsContainer extends HashMap<String, ArrayList<String>> {

    @Override
    public ArrayList<String> put(String key, ArrayList<String> value) {

        if (this.get(key) != null && this.get(key).size() != 0) {
            this.get(key).addAll(value);
            return null;
        }
        return super.put(key, new ArrayList<>(value));
    }

    @Override
    public int size() {
        int result = 0;
        for (ArrayList<String> value : this.values()) {
            if (value == null) {
                continue;
            }
            result += value.size();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("{");

        boolean firstLoop = true;

        ArrayList<String> sortedKeySet = new ArrayList<>(this.keySet());
        sortedKeySet.remove(null);  // since it used be a set this removes all nulls
        sortedKeySet.sort(null);

        for (String key : sortedKeySet) {

            if (this.get(key) == null || this.get(key).size() == 0) {
                continue;
            }


            if (!firstLoop) {
                result.append(",");
            }


            result.append("\"").append(key).append("\"");
            result.append(":[");

            boolean firstInnerLoop = true;
            for (String i : get(key) ) {
                if(!firstInnerLoop){
                    result.append(",");
                }
                try {
                    result.append(new ObjectMapper().writeValueAsString(i));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                firstInnerLoop = false;
            }
            result.append("]");

            firstLoop = false;
        }

        result.append("}");
        return result.toString();
    }


    public String createBadRequestAPIResponseBody(String key, Message correspondingMessage){
        String result;

        String template = "{\n" +
            "    \"messageAction\": \"replaceWithMessageAction\",\n" +
            "    \"messageContent\": {\n" +
            "        \"The service is not conformant\": \n" +
            "            replaceWithMessageContent\n" +
            "    },\n" +
            "    \"messageKey\": \"replaceWithMessageKey\",\n" +
            "    \"messageNumber\": \"replaceWithMessageNumber\",\n" +
            "    \"messageReason\": \"replaceWithMessageReason\",\n" +
            "    \"messageType\": \"replaceWithMessageType\"\n" +
            "}" ;

        result = template.replace("replaceWithMessageKey", key);
        result = result.replace("replaceWithMessageContent", this.toString());

        result = result.replace("replaceWithMessageReason", correspondingMessage.mapToApiMessage().getMessageReason());
        result = result.replace("replaceWithMessageNumber", correspondingMessage.mapToApiMessage().getMessageNumber());
        result = result.replace("replaceWithMessageType", correspondingMessage.mapToApiMessage().getMessageType().toString());
        result = result.replace("replaceWithMessageAction", correspondingMessage.mapToApiMessage().getMessageAction());

        return result;

    }

}
