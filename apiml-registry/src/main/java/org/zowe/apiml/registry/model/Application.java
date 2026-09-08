/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** All known instances of one service. */
public final class Application {

    private final String name;
    private final List<ServiceInstance> instances;

    public Application(String name, List<ServiceInstance> instances) {
        this.name = name;
        this.instances = Collections.unmodifiableList(new ArrayList<>(instances));
    }

    public String name() {
        return name;
    }

    public List<ServiceInstance> instances() {
        return instances;
    }

    public int size() {
        return instances.size();
    }

    @Override
    public String toString() {
        return "Application[" + name + " x" + instances.size() + "]";
    }

}
