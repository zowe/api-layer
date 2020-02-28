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

import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;

public class ZoweVersionProducerTest {
    @Test
    public void givenValidManifest_whenVersionIsRequested_thenProvideTheBuildInformation() throws Exception {
        File file = ResourceUtils.getFile(CLASSPATH_URL_PREFIX + "zowe-manifest.json");
        Version validZoweVersion = new ZoweVersionProducer(file).version();
        Version expectedVersion = new Version("1.8.0", "802", "397a4365056685d639810a077a58b736db9f018b");
        assertThat(validZoweVersion, is(expectedVersion));
    }

    @Test
    public void givenManifestWithoutBuildInfo_whenVersionIsRequested_thenNullIsReturned() throws Exception {
        File file = ResourceUtils.getFile(CLASSPATH_URL_PREFIX + "zowe-manifest-invalid.json");
        Version validZoweVersion = new ZoweVersionProducer(file).version();
        assertThat(validZoweVersion, is(nullValue()));
    }
}
