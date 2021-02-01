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

import org.zowe.apiml.client.services.apars.PHBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Versions {
    private final Apars apars = new Apars();

    private final Map<String, List<Apar>> aparsAppliedForVersion = new HashMap<>();

    public Versions() {
        ArrayList<Apar> baseApars = new ArrayList<>();
        baseApars.add(new PHBase());

        aparsAppliedForVersion.put("2.3", baseApars);
        aparsAppliedForVersion.put("2.4", baseApars);
    }

    public List<Apar> baselineForVersion(String version) {
        return aparsAppliedForVersion.get(version);
    }

    public List<Apar> fullSetOfApplied(String baseVersion, String[] appliedApars) {
        List<Apar> baseline = baselineForVersion(baseVersion);
        baseline.addAll(apars.getApars(appliedApars));
        return baseline;
    }
}
