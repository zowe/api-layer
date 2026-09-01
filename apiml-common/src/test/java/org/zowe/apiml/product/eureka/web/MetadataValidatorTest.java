/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka.web;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class MetadataValidatorTest {

    private MetadataValidator metadataValidator;

    @BeforeEach
    void setUp() {
        metadataValidator = new MetadataValidator(null, null, null, false);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "localhost,192.168.0.2,example.com|192.168.0.2|true",
        "localhost,192.168.0.2,example.com|192.168.0.1|false",
        "localhost|127.0.0.1|true",
        "localhost|invalid#1|false",
        "[::1]|[::1]|true",
        "[::1]|[::2]|false",
        "[2001:db8::1]|[2001:db8::1]|true",
        "[2001:db8::1]|[2001:db8::2]|false",
        "2001:db8::/32|[2001:db8:abcd:1234::1]|true",
        "2001:db8::/32|[2001:db9::1]|false",
        "[fe80::]/10|[fe80::1234:5678:9abc]|true",
        "[fe80::]/10|[fc00::1]|false",
        "2001:db8::/64|[2001:db8:0:0:ffff:ffff:ffff:ffff]|true",
        "2001:db8::/64|[2001:db8:0:1::1]|false",
        "localhost,192.168.0.0/24,2001:db8::/32|192.168.0.10|true",
        "localhost,192.168.0.0/24,2001:db8::/32|[2001:db8::abcd]|true",
        "localhost,192.168.0.0/24,2001:db8::/32|127.0.0.1|true",
        "localhost,192.168.0.0/24,2001:db8::/32|10.0.0.1|false",
        "localhost,192.168.0.0/24,2001:db8::/32|[fe80::1]|false",
        "192.168.0.2|192.168.0.2|true",
        "192.168.0.2|192.168.0.3|false",
        "192.168.0.0/24|192.168.0.55|true",
        "192.168.0.0/24|192.168.1.1|false",
        "10.0.0.0/8|10.255.255.255|true",
        "10.0.0.0/8|11.0.0.1|false",
        "172.16.0.0/12|172.31.255.254|true",
        "172.16.0.0/12|172.32.0.1|false",
        "192.168.0.0/32|192.168.0.0|true",
        "192.168.0.0/32|192.168.0.1|false",
        "192.168.0.0/abc|192.168.0.0|false"
    })
    void givenIpAddressInAllowedList_whenIsAllowedDomain_thenDecide(String allowList, String domain, boolean isAllowed) {
        ReflectionTestUtils.setField(metadataValidator,"allowedDomainsSet", new HashSet<>(Arrays.asList(allowList.split(","))));
        assertEquals(isAllowed, metadataValidator.isAllowedDomain(domain, false));
    }

}
