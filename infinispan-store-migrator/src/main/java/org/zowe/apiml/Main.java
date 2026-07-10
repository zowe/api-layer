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

import org.infinispan.tools.store.migrator.StoreMigrator;

public class Main {
    public static void main(String[] args) throws Exception {
       migrate("infinispan-store-migrator/config/migrator-invalidatedJwtTokens.properties");
       migrate("infinispan-store-migrator/config/migrator-zoweCache.properties");
       migrate("infinispan-store-migrator/config/migrator-zoweInvalidatedTokenCache.properties");
    }

    private static void migrate(String properties) throws Exception {
        System.out.println("Migrating " + properties + "...");
        StoreMigrator.main(new String[]{properties});
        System.out.println("Migration using " + properties + " is completed.");
    }
}
