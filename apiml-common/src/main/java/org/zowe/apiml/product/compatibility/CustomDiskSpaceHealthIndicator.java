/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.compatibility;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.util.unit.DataSize;

import java.io.File;

/**
 * Custom implementation that extends DiskSpaceHealthIndicator to prevent
 * misleading disk space logs for z/OS
 */
public class CustomDiskSpaceHealthIndicator extends DiskSpaceHealthIndicator {

    private final File path;
    private final DataSize threshold;

    /**
     * Create a custom DiskSpaceHealthIndicator that overrides the default behavior
     * to prevent misleading logs for z/OS
     */
    public CustomDiskSpaceHealthIndicator(File path, DataSize threshold) {
        super(path, threshold);
        this.path = path;
        this.threshold = threshold;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        // Always reporting UP status without checking disk space on z/OS
        builder.up()
            .withDetail("total", "not monitored")
            .withDetail("free", "not monitored")
            .withDetail("threshold", this.threshold.toBytes())
            .withDetail("path", this.path.getAbsolutePath())
            .withDetail("exists", this.path.exists())
            .withDetail("note", "Disk space monitoring disabled");
    }
}
