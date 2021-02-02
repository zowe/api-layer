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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.client.services.apars.PHBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Versions {
    private final Apars apars;
    private final Map<String, List<Apar>> aparsAppliedForVersion = new HashMap<>();

    @Autowired
    public Versions(@Value("${zosmf.username}") List<String> usernames, @Value("${zosmf.password}") List<String> passwords) {
        this.apars = new Apars(usernames, passwords);

        ArrayList<Apar> baseApars = new ArrayList<>();
        baseApars.add(new PHBase(usernames, passwords));

        aparsAppliedForVersion.put("2.3", baseApars);
        aparsAppliedForVersion.put("2.4", baseApars);
    }

    public List<Apar> baselineForVersion(String version) throws Exception{
        List<Apar> appliedForVersion = aparsAppliedForVersion.get(version);

        if (appliedForVersion == null) {
            throw new Exception("Invalid version '" + version + "' given for baseline APARs");
        }

        // New list to avoid changes in aparsAppliedForVersion caused by fullSetOfApplied
        return new ArrayList<>(appliedForVersion);
    }

    public List<Apar> fullSetOfApplied(String baseVersion, List<String> appliedApars) throws Exception {
        List<Apar> baseline = baselineForVersion(baseVersion);
        baseline.addAll(apars.getApars(appliedApars));
        return baseline;
    }
}
