/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.zowe.apiml.util.service.FullApiMediationLayer;

@Slf4j
public class StartAndCleanApplications implements TestExecutionListener {
    private FullApiMediationLayer fullApiMediationLayer;

    public StartAndCleanApplications() {
        fullApiMediationLayer = FullApiMediationLayer.getInstance();
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (!fullApiMediationLayer.runsOffPlatform()) {
            log.info("Starting Full API Mediation Layer");
            fullApiMediationLayer.start();
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        log.info("Stopping Full API Mediation Layer");
        fullApiMediationLayer.stop();
    }

}
