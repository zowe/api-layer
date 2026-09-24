/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.Lease;
import org.zowe.apiml.registry.model.ServiceInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rewrite has to reach renew and cancel as well as registration - a service that registered under the old
 * prefix keeps heartbeating under it, and those heartbeats have to find the rewritten entry or the instance is
 * evicted while it is healthy. These cover the rewrite itself; the wrapping of every write is
 * {@link PrefixRewritingServiceRegistry}'s job.
 */
class ServiceIdPrefixRewriterTest {

    private static final String TUPLE = "OLDSERVICE,NEWSERVICE";

    private static ServiceInstance instance(String appName, String instanceId) {
        return ServiceInstance.builder()
            .instanceId(instanceId)
            .appName(appName)
            .hostName("localhost")
            .ipAddr("127.0.0.1")
            .port(10010, true)
            .vipAddress("anything")
            .status(InstanceStatus.UP)
            .lease(Lease.renewable(30, 90, System.currentTimeMillis()))
            .build();
    }

    @Test
    void givenAConfiguredPair_whenItIsAValidPair_thenTheRewriterIsEnabled() {
        assertThat(new ServiceIdPrefixRewriter(TUPLE).enabled()).isTrue();
    }

    @Test
    void givenNoUsablePair_whenItIsDisabled_thenTheRewriterSaysSo() {
        assertThat(new ServiceIdPrefixRewriter(null).enabled()).isFalse();
        assertThat(new ServiceIdPrefixRewriter("").enabled()).isFalse();
        assertThat(new ServiceIdPrefixRewriter("OLDSERVICE").enabled()).isFalse();
        assertThat(new ServiceIdPrefixRewriter("OLDSERVICE,OLDSERVICE").enabled()).isFalse();
    }

    @Test
    void givenADisabledRewriter_whenAPairIsRewritten_thenItComesBackUnchanged() {
        assertThat(new ServiceIdPrefixRewriter("OLDSERVICE").rewrite("OLDSERVICE", "localhost:OLDSERVICE:10010"))
            .containsExactly("OLDSERVICE", "localhost:OLDSERVICE:10010");
    }

    @Test
    void givenAnEnabledRewriter_whenAPairIsRewritten_thenBothPartsChange() {
        assertThat(new ServiceIdPrefixRewriter(TUPLE).rewrite("OLDSERVICE", "localhost:OLDSERVICE:10010"))
            .containsExactly("NEWSERVICE", "localhost:NEWSERVICE:10010");
    }

    @Test
    void givenAnAppNameInLowerCase_whenItIsRewritten_thenTheResultIsUpperCase() {
        assertThat(new ServiceIdPrefixRewriter(TUPLE).rewrite("oldservice", "localhost:oldservice:10010"))
            .containsExactly("NEWSERVICE", "localhost:NEWSERVICE:10010");
    }

    @Test
    void givenAnInstanceCarryingTheOldPrefix_whenItIsRewritten_thenItsIdentifiersFollowAndTheRestIsKept() {
        var rewriter = new ServiceIdPrefixRewriter(TUPLE);
        var original = instance("OLDSERVICE", "localhost:OLDSERVICE:10010");

        var rewritten = rewriter.rewrite(original);

        assertThat(rewritten.appName()).isEqualTo("NEWSERVICE");
        assertThat(rewritten.appGroupName()).isEqualTo("NEWSERVICE");
        assertThat(rewritten.instanceId()).isEqualTo("localhost:NEWSERVICE:10010");
        assertThat(rewritten.vipAddress()).isEqualTo("newservice");
        assertThat(rewritten.port()).isEqualTo(original.port());
        assertThat(rewritten.ipAddr()).isEqualTo(original.ipAddr());
        assertThat(rewritten.status()).isEqualTo(InstanceStatus.UP);
    }

    @Test
    void givenAnInstanceWithoutTheOldPrefix_whenItIsRewritten_thenItIsPassedThrough() {
        var original = instance("OTHERSERVICE", "localhost:OTHERSERVICE:10010");

        assertThat(new ServiceIdPrefixRewriter(TUPLE).rewrite(original)).isSameAs(original);
    }
}
