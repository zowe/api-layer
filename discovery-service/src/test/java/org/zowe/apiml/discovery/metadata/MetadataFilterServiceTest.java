/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataFilterServiceTest {

    private MetadataFilterService metadataFilterService;

    @BeforeEach
    void setUp() {
        metadataFilterService = new MetadataFilterService();
    }

    @Test
    void testAfterPropertiesSet() {

    }

    @Test
    void testIsAllowedDomain() {

    }

    @Test
    void testVerifyAllowedDomains() {

    }
}
