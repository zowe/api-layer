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

import lombok.extern.slf4j.Slf4j;
import org.infinispan.tools.store.migrator.StoreMigrator;

@Slf4j
public class Main {
    public static void main(String[] args) {
       migrate("infinispan-store-migrator/config/migrator-invalidatedJwtTokens.properties");
       migrate("infinispan-store-migrator/config/migrator-zoweCache.properties");
       migrate("infinispan-store-migrator/config/migrator-zoweInvalidatedTokenCache.properties");
    }

    private static void migrate(String properties) {
        log.info("Migrating {}...", properties);

        try {
            StoreMigrator.main(new String[]{properties});
            log.info("Migration using {} is completed.", properties);
        } catch (Exception e) {
            log.error(
                "Migration failed for {}. Continuing with the remaining cache stores. " +
                    "The source Soft Index File Store could not be fully read. " +
                    "The store may be incomplete or inconsistent.",
                properties,
                e
            );
        }
    }
}
