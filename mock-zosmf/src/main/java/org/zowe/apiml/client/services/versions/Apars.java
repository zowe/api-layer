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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zowe.apiml.client.services.apars.DefaultApar;
import org.zowe.apiml.client.services.apars.PH12143;
import org.zowe.apiml.client.services.apars.PH30398;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Apars {
    private final Map<String, Apar> availableApars = new HashMap<>();

    @Autowired
    public Apars(PH12143 ph12143, PH30398 ph30398) {
        availableApars.put("PH12143", ph12143);
        availableApars.put("PH17867", new DefaultApar());
        availableApars.put("PH28507", new DefaultApar());
        availableApars.put("PH28532", new DefaultApar());
        availableApars.put("PH30398", ph30398);
    }

    public List<Apar> getApars(String[] names) {
        ArrayList<Apar> result = new ArrayList<>();
        for (String name : names) {
            if (availableApars.containsKey(name)) {
                result.add(availableApars.get(name));
            }
        }

        return result;
    }
}
