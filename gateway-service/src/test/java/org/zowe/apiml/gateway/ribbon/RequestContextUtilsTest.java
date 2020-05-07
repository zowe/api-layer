/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.zowe.apiml.gateway.ribbon.RequestContextUtils.INSTANCE_INFO_KEY;

class RequestContextUtilsTest {

    @BeforeEach
    void setUp() {
        RequestContext.getCurrentContext().clear();
    }

    @Test
    void givenRequestContext_whenRequestContextHasInfo_thenRetrieveIt() {
        InstanceInfo info = InstanceInfo.Builder.newBuilder().setAppName("Francesca").build();
        RequestContext.getCurrentContext().set(INSTANCE_INFO_KEY, info);
        assertThat(RequestContextUtils.getInstanceInfo().isPresent(), is(true));
        assertThat(RequestContextUtils.getInstanceInfo().get(), is(info));
    }

    @Test
    void givenRequestContext_whenNoInfo_thenRetrieveEmpty() {
        assertThat(RequestContextUtils.getInstanceInfo().isPresent(), is(false));
    }

    @Test
    void givenEmptyContext_whenInfoStored_thenStoreInfo() {
        InstanceInfo info = InstanceInfo.Builder.newBuilder().setAppName("Vanessa").build();
        assertThat(RequestContext.getCurrentContext().get(INSTANCE_INFO_KEY), is(nullValue()));
        RequestContextUtils.setInstanceInfo(info);
        assertThat(RequestContext.getCurrentContext().get(INSTANCE_INFO_KEY), is(info));
    }

    @Test
    void givenContextWithInstanceInfo_whenSetNull_thenRetrieveEmpty() {
        InstanceInfo info = InstanceInfo.Builder.newBuilder().setAppName("Vanessa").build();
        RequestContext.getCurrentContext().set(INSTANCE_INFO_KEY, info);
        RequestContextUtils.setInstanceInfo(null);
        assertThat(RequestContextUtils.getInstanceInfo().isPresent(), is(false));
    }
}
