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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

import javax.annotation.Nonnull;

import java.util.Map;

public abstract class ApimlOpenTelemetryResourceProvider implements ResourceProvider {

    /*
    # service attributes: link to opentelemetry semantic conventionservice.name - logical name of the service, must be same for all instances of horizontally scaled services (e.g. instances within the same HA deployment). Expected to be globally unique if namespace is not defined.

# service.instance.id - must be unique for each instance of service.name + service.namespace pair. Automatically generated uuid is generally recommended to ensure uniqueness.

# service.namespace - value having a meaning that helps to distinguish a group of services, e.g. lpar, owner team, etc. service.name is expected to be unique within the same namespace.

# service.version

# deployment attributes: linkdeployment.environment.name - dev/test/staging/production

# z/OS attributes: linkzos.smf.id - The System Management Facility (SMF) Identifier uniquely identified a z/OS system within a SYSPLEX or mainframe environment and is used for system and performance analysis.

# zos.sysplex.name - The name of the SYSPLEX to which the z/OS system belongs too.

# mainframe.lpar.name - Name of the logical partition that hosts a systems with a mainframe operating system.

# os.type - The operating system type, e.g. zos

# os.version - The version string of the operating system. On z/OS, SHOULD be the release returned by the command d iplinfo.

# process.command - The command used to launch the process (i.e. the command name). On z/OS, SHOULD be set to the name of the job used to start the z/OS system software.

# process.pid - Process identifier (PID). On z/OS, SHOULD be set to the Address Space Identifier.

    */

    abstract Map<String, String> getOsAttributes();

    @Override
    public Resource createResource(@Nonnull ConfigProperties config) {
        var attributesBuilder = Attributes.builder();

        // Are the OS-specific attributes already provided by the Spring boot starter when non-zos? what about when it's on zos?
        attributesBuilder.put("process.pid", "null"); // Should be Address Space Identifier (can we get it from JZOS) or simple PID if running non-zos
        attributesBuilder.put("process.command", "null"); // Should be name of the Job used to start the z/OS system software (STC / BPX JOBNAME?)
        attributesBuilder.put("os.version", "null"); // d iplinfo
        attributesBuilder.put("os.type", "null"); // if running on zos, zos, otherwise?
        attributesBuilder.put("os.type", "null"); // if running on zos, zos, otherwise?

        return Resource.create(attributesBuilder.build());
    }

}
