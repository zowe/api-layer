/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

/**
 * Set of attributes set by API ML
 */
public class ZosOpenTelemetryAttributes {

    public static final String OTEL_ZOS_JOBNAME = "process.zos.jobname";
    public static final String OTEL_ZOS_USERID = "process.zos.userid";
    public static final String OTEL_ZOS_JOBID = "process.zos.jobid";
    public static final String OTEL_ZOS_INSTANCE_ID = "service.instance.id";
    public static final String OTEL_OS_VERSION = "os.version";

}
