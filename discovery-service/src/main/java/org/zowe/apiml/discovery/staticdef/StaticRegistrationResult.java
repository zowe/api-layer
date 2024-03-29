/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.staticdef;

import com.netflix.appinfo.InstanceInfo;
import lombok.Data;
import org.zowe.apiml.message.core.Message;

import java.util.*;

/**
 * Result of registration of static services
 * Contains registered services, additional metadata, errors ...
 */
@Data
public class StaticRegistrationResult {
    private final List<Message> errors = new LinkedList<>();
    private final List<InstanceInfo> instances = new LinkedList<>();
    private final Map<String, ServiceOverrideData> additionalServiceMetadata = new HashMap<>();
    private final List<String> registeredServices = new LinkedList<>();
}
