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

@Slf4j
public class StartAndCleanApplications implements TestExecutionListener {
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        log.info("TestPlan Execution Started: %s", testPlan);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        log.info("TestPlan Execution Finished: %s", testPlan);
    }

}
