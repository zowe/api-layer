/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.client.services.apars.PH12143;
import org.zowe.apiml.client.services.apars.PH30398;
import org.zowe.apiml.client.services.versions.Apars;
import org.zowe.apiml.client.services.versions.Versions;

class AparBasedServiceTest {
    private AparBasedService underTest;

    @BeforeEach
    void setUp() {
        underTest = new AparBasedService(new Versions(new Apars(new PH12143(), new PH30398())));
    }

    @Nested
    class whenProcessing {
        @Test
        void givenInvalidVersion_InternalServerErrorIsReturned() {

        }

        @Test
        void givenValidVersionButInvalidApars_resultsProperlyHandled() {

        }

        @Test
        void givenValidVersionAndApars_resultIsProperlyHandled() {

        }
    }
}
