/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.codec;

/**
 * Literals that appear verbatim on the wire.
 * <p>
 * Several of these are Netflix class names. They are <em>data</em>, not code references - deployed Eureka clients
 * match on these exact strings when deciding how to deserialise a payload, so they must keep being emitted even
 * though the classes they name no longer exist anywhere in this codebase. Changing them silently breaks every
 * already-onboarded service. See TASK-Discovery-Native-Registry.md section 4.2.
 */
public final class WireConstants {

    /** Emitted as the {@code dataCenterInfo} type discriminator: {@code @class} in JSON, {@code class} in XML. */
    public static final String DATA_CENTER_INFO_CLASS = "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo";

    /**
     * Emitted as the sole key of {@code metadata} when a JSON instance has no metadata at all.
     * <p>
     * Eureka serialised an empty metadata map as {@code {"@class":"java.util.Collections$EmptyMap"}} rather than
     * as {@code {}}. XML instead emits an empty {@code <metadata/>} element. Both are reproduced.
     */
    public static final String EMPTY_METADATA_CLASS = "java.util.Collections$EmptyMap";

    public static final String TYPE_DISCRIMINATOR_JSON = "@class";
    public static final String TYPE_DISCRIMINATOR_XML = "class";

    /** JSON encodes a port as an object: the number under {@code $}, the flag under {@code @enabled}. */
    public static final String PORT_VALUE_KEY = "$";
    public static final String PORT_ENABLED_KEY = "@enabled";
    /** XML encodes the same thing as element text with an {@code enabled} attribute. */
    public static final String PORT_ENABLED_ATTRIBUTE = "enabled";

    public static final String ROOT_INSTANCE = "instance";
    public static final String ROOT_APPLICATION = "application";
    public static final String ROOT_APPLICATIONS = "applications";

    public static final String FIELD_VERSIONS_DELTA = "versions__delta";
    public static final String FIELD_APPS_HASHCODE = "apps__hashcode";
    public static final String FIELD_REGISTERED_APPLICATIONS_EMPTY = "registeredApplicationsEmpty";

    private WireConstants() {
    }

}
