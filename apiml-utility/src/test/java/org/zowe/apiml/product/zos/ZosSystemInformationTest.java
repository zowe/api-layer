/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.zos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZosSystemInformationTest {

    @Mock
    private ZUtilDummy zUtil;

    private ZosSystemInformation zosSystemInformation;

    @BeforeEach
    void setUp() {
        zosSystemInformation = new ZosSystemInformation();
        ReflectionTestUtils.setField(zosSystemInformation, "zUtil", zUtil);
    }

    @Test
    void givenZos_thenGetAttributes() {
        when(zUtil.getCurrentJobId()).thenReturn("JOBID");
        when(zUtil.getCurrentJobname()).thenReturn("JOBNAME");
        when(zUtil.getCurrentUser()).thenReturn("USER");
        when(zUtil.getPid()).thenReturn(123456);
        when(zUtil.substituteSystemSymbols("&SYSNAME.")).thenReturn("SYSNAME");
        when(zUtil.substituteSystemSymbols("&SYSCLONE.")).thenReturn("32");
        when(zUtil.substituteSystemSymbols("&SYSPLEX.")).thenReturn("PLEX");
        when(zUtil.substituteSystemSymbols("&SMFID.")).thenReturn("LP32");
        when(zUtil.substituteSystemSymbols("&ENVIRON.")).thenReturn("PROD");
        when(zUtil.substituteSystemSymbols("&OSLEVEL.")).thenReturn("030200");

        var data = zosSystemInformation.get();
        assertFalse(data.isEmpty());
    }

    @Test
    void givenZos_whenNoSymbol_thenIgnored() {
        when(zUtil.getCurrentJobId()).thenReturn("JOBID");
        when(zUtil.getCurrentJobname()).thenReturn("JOBNAME");
        when(zUtil.getCurrentUser()).thenReturn("USER");
        when(zUtil.getPid()).thenReturn(123456);
        when(zUtil.substituteSystemSymbols("&ENVIRON.")).thenReturn("&ENVIRON.");
        when(zUtil.substituteSystemSymbols("&OSLEVEL.")).thenReturn(null);
        when(zUtil.substituteSystemSymbols("&SMFID.")).thenReturn("");
        when(zUtil.substituteSystemSymbols("&SYSPLEX.")).thenReturn("&sysplex.");
        when(zUtil.substituteSystemSymbols("&SYSNAME.")).thenReturn(null);
        when(zUtil.substituteSystemSymbols("&SYSCLONE.")).thenReturn(null);

        var data = zosSystemInformation.get();
        assertFalse(data.isEmpty());
        assertEquals("", data.get("zos.environ"));
        assertEquals("", data.get("zos.version"));
        assertEquals("", data.get("zos.smfid"));
        assertEquals("", data.get("zos.sysplex"));
        assertEquals("", data.get("zos.sysname"));
        assertEquals("", data.get("zos.sysclone"));
    }

}
