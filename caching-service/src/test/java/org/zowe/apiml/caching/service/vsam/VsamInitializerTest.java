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
import org.junit.jupiter.api.Nested;
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
    private VsamRecord record;
    private VsamInitializer underTest;

    private ZFile zFile;

    @BeforeEach
    void setUp() {
        vsamConfiguration = DefaultVsamConfiguration.defaultConfiguration();
        record = new VsamRecord(vsamConfiguration, "delete", new KeyValue("me", "novalue"));
        underTest = new VsamInitializer();

        zFile = mock(ZFile.class);
    }

    @Nested
    class WhenInitializing {
        @Test
        void givenValidZFileBehavior_thenRecordIsInsertedAndDeleted() throws ZFileException, VsamRecordException {
            when(zFile.locate(record.getKeyBytes(), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(true);
            underTest.warmUpVsamFile(zFile, vsamConfiguration);

            verify(zFile).write(any());
            verify(zFile).read(any());
            verify(zFile).delrec();
        }

        @Test
        void givenZFileNotLocated_thenRecordIsNotDeleted() throws ZFileException, VsamRecordException {
            when(zFile.locate(record.getKeyBytes(), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(false);
            underTest.warmUpVsamFile(zFile, vsamConfiguration);

            verify(zFile, times(1)).write(any());
            verify(zFile, times(0)).read(any());
            verify(zFile, times(0)).delrec();
        }
    }
}
