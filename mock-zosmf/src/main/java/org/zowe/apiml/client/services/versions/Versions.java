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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.client.services.apars.PHBase;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Versions {
    private Apars apars;
    private final Map<String, List<Apar>> aparsAppliedForVersion = new HashMap<>();

    // TODO Verify what values are these points.
    @Value("${zosmf.username}")
    private List<String> usernames;
    @Value("${zosmf.password}")
    private List<String> passwords;

    @PostConstruct
    void setBaseApars() {
        apars = new Apars(usernames, passwords);

        ArrayList<Apar> baseApars = new ArrayList<>();
        baseApars.add(new PHBase(usernames, passwords));

        aparsAppliedForVersion.put("2.3", baseApars);
        aparsAppliedForVersion.put("2.4", baseApars);
    }

    public List<Apar> baselineForVersion(String version) {
        // New list to avoid changes in aparsAppliedForVersion
        return new ArrayList<>(aparsAppliedForVersion.get(version));
    }

    public List<Apar> fullSetOfApplied(String baseVersion, String[] appliedApars) {
        List<Apar> baseline = baselineForVersion(baseVersion);
        baseline.addAll(apars.getApars(appliedApars));
        return baseline;
    }
}
