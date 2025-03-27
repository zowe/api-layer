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

import org.springframework.modulith.core.ApplicationModules;

public class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(ApimlApplication.class);

    // @Test
    // void verifiesModularStructure() {
    //     modules.verify();
    // }

    // @Test
    // void createModuleDocumentation() {
    //     new Documenter(modules).writeDocumentation();
    // }

}
