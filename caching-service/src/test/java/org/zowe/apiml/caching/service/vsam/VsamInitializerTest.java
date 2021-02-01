/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service.vsam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.zfile.ZFile;
import org.zowe.apiml.zfile.ZFileConstants;
import org.zowe.apiml.zfile.ZFileException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VsamInitializerTest {
    private VsamConfig vsamConfiguration;
    private VsamInitializer underTest;

    @BeforeEach
    void setUp() {
        vsamConfiguration = DefaultVsamConfiguration.defaultConfiguration();
        underTest = new VsamInitializer();
    }

    @Test
    void givenValidZfileBehavior_whenInitializing_thenRecordIsInsertedAndDeleted() throws ZFileException, VsamRecordException {
        VsamRecord record = new VsamRecord(vsamConfiguration, "delete", new KeyValue("me", "novalue"));

        ZFile zFile = mock(ZFile.class);
        when(zFile.locate(record.getKeyBytes(), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(true);

        underTest.warmUpVsamFile(zFile, vsamConfiguration);

        verify(zFile).write(any());
        verify(zFile).read(any());
        verify(zFile).delrec();
    }
}
