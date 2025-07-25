/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties("apiml.caching.eureka.instance")
@Data
public class CachingServiceEurekaInstanceConfigBean {

    /**
     * Gets the metadata name/value pairs associated with this instance. This information
     * is sent to eureka server and can be used by other instances.
     */
    private Map<String, String> metadataMap = new HashMap<>();

}
