/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry;

/**
 * What this process registered itself as.
 * <p>
 * Small on purpose. Code that needs to recognise its own registration in the registry - to skip itself when
 * broadcasting to peers, mostly - needs the identity and nothing else, and asking for the whole client just to
 * read one field is what made those call sites impossible to run outside a fully wired Eureka context.
 * <p>
 * Two implementations: the standalone services build one from {@code eureka.instance.*}, and the modulith
 * supplies its Gateway registration, since inside the modulith one process is several services at once.
 */
public interface SelfRegistration {

    String instanceId();

    String serviceId();

}
