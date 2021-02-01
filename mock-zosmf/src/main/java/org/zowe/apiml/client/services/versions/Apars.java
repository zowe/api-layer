/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.services.versions;

import org.zowe.apiml.client.services.apars.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Apars {
    private final Map<String, Apar> availableApars = new HashMap<>();

    public Apars() {

        availableApars.put("PH12143", new PH12143());
        availableApars.put("PH17867", new DefaultApar());
        availableApars.put("PH28507", new DefaultApar());
        availableApars.put("PH28532", new DefaultApar());
        availableApars.put("PH30398", new PH30398());
    }

    public List<Apar> getApars(String[] names) {
        ArrayList<Apar> result = new ArrayList<>();
        for(String name: names) {
            if (availableApars.containsKey(name)) {
                result.add(availableApars.get(name));
            }
        }

        return result;
    }
}
