/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.version;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FileBasedVersionsTest {
    private VersionProducer zoweVersionProducer;
    private VersionProducer apiMlVersionProducer;

    private FileBasedVersions underTest;


    @BeforeEach
    public void setup() {
        zoweVersionProducer = mock(VersionProducer.class);
        apiMlVersionProducer = mock(VersionProducer.class);

        underTest = new FileBasedVersions(apiMlVersionProducer, zoweVersionProducer);
    }

    @Test
    public void givenValidProducers_whenTheVersionIsRequested_thenTheVersionIsProvidedAsValidCompoundInfo() {
        Version zoweVersion = new Version("1.9.0", "123", "13f5g7");
        Version apiMlVersion = new Version("1.3.0", "345", null);
        when(zoweVersionProducer.version()).thenReturn(zoweVersion);
        when(apiMlVersionProducer.version()).thenReturn(apiMlVersion);

        VersionInfo versionDetails = underTest.getVersion();

        assertThat(versionDetails, is(new VersionInfo(zoweVersion, apiMlVersion)));
    }

    @Test
    public void givenTheVersionWasAlreadyReceived_whenTheVersionIsRequested_thenTheCachedVersionIsProvided() {
        Version zoweVersion = new Version("1.9.0", "123", "13f5g7");
        Version apiMlVersion = new Version("1.3.0", "345", null);
        when(zoweVersionProducer.version()).thenReturn(zoweVersion);
        when(apiMlVersionProducer.version()).thenReturn(apiMlVersion);
        underTest.getVersion();

        when(zoweVersionProducer.version()).thenReturn(new Version("1.10.0", "234", "13f5g7"));
        VersionInfo cached = underTest.getVersion();
        assertThat(cached, is(new VersionInfo(zoweVersion, apiMlVersion)));
    }
}
