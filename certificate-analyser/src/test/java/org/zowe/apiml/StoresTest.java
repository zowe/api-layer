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

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoresTest {

    @Test
    void providedWrongPassword_thenStoresNotInitializeExceptionIsThrown() {
        String[] args = {"--keystore", "../keystore/localhost/localhost.keystore.p12",
            "--truststore", "../keystore/localhost/localhost.truststore.p12",
            "--keypasswd", "wrongPass",
            "--keyalias", "localhost"};
        ApimlConf conf = new ApimlConf();
        new CommandLine(conf).parseArgs(args);
        StoresNotInitializeException e = assertThrows(StoresNotInitializeException.class, () -> new Stores(conf));
        assertEquals("keystore password was incorrect",e.getMessage());
    }


}
