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


            result.append(key);
            result.append(":");
            result.append(get(key).toString());

            firstLoop = false;
        }


        return result.toString();
    }


}
