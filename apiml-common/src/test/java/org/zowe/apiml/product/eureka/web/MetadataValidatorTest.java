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

import com.netflix.appinfo.InstanceInfo;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MetadataValidatorTest {

    @Mock
    private InstanceInfo instanceInfo;

    private MetadataValidator metadataValidator;

    @BeforeEach
    void setUp() {
        lenient().when(instanceInfo.getSecurePort()).thenReturn(8080);
        metadataValidator = new MetadataValidator(instanceInfo, null, null, false, false);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "localhost,192.168.0.2:*,example.com|192.168.0.2|true|true",
        "localhost,192.168.0.2,example.com|192.168.0.1|false|false",
        "localhost:8080|127.0.0.1:8080|true|true",
        "localhost|invalid#1|false|true",
        "[::1]:*|[::1]:8080|true|true",
        "[::1]:*|[::2]|false|true",
        "[2001:db8::1]:8080|[2001:db8::1]:8080|true|true",
        "[2001:db8::1]|[2001:db8::2]|false|true",
        "2001:db8::/32|[2001:db8:abcd:1234::1]:8080|true|false",
        "2001:db8::/32|[2001:db8:abcd:1234::1]:8080|false|true",
        "2001:db8::/32|[2001:db9::1]|false|true",
        "[fe80::]/10|[fe80::1234:5678:9abc]:8080|true|false",
        "[fe80::]/10|[fe80::1234:5678:9abc]:8080|false|true",
        "[fe80::]/10|[fc80::1234:5678:9abc]:8080|false|false",
        "[fe80::]/10|[fc00::1]|false|true",
        "2001:db8::/64|[2001:db8:0:0:ffff:ffff:ffff:ffff]:8080|true|false",
        "2001:db8::/64|[2001:db8:0:0:ffff:ffff:ffff:ffff]:8080|false|true",
        "2001:db8::/64|[2001:fc8:0:0:ffff:ffff:ffff:ffff]:8080|false|false",
        "2001:db8::/64|[2001:db8:0:1::1]|false|true",
        "localhost,192.168.0.0/24,2001:db8::/32|192.168.0.10:8080|true|false",
        "localhost,192.168.0.0/24,2001:db8::/32|192.168.0.10:8080|false|true",
        "localhost,192.168.0.0/24,2001:db8::/32|10.0.0.10:8080|false|false",
        "localhost,192.168.0.0/24,2001:db8::/32|[2001:db8::abcd]|true|false",
        "localhost:*,192.168.0.0/24,2001:db8::/32|127.0.0.1:8080|true|true",
        "localhost,192.168.0.0/24,2001:db8::/32|10.0.0.1|false|true",
        "localhost,192.168.0.0/24,2001:db8::/32|[fe80::1]|false|true",
        "192.168.0.2:*|192.168.0.2:8080|true|true",
        "192.168.0.2|192.168.0.3|false|true",
        "192.168.0.0/24|192.168.0.55:8080|true|false",
        "192.168.0.0/24|192.168.0.55:8080|false|true",
        "192.168.0.0/24|192.168.1.1|false|true",
        "10.0.0.0/8|10.255.255.255:8080|true|false",
        "10.0.0.0/8|10.255.255.255:8080|false|true",
        "10.0.0.0/8|11.0.0.1|false|true",
        "172.16.0.0/12|172.31.255.254:8080|true|false",
        "172.16.0.0/12|172.31.255.254:8080|false|true",
        "172.16.0.0/12|172.32.0.1|false|true",
        "192.168.0.0/32|192.168.0.0:8080|false|true",
        "192.168.0.0/32|192.168.0.0:8080|true|false",
        "192.168.0.0/32|192.132.0.0:8080|false|false",
        "192.168.0.0/32|192.168.0.1|false|true",
        "192.168.0.0/abc|192.168.0.0|false|true",
        "192.168.0.1:8080|192.168.0.1:8080|true|true",
        "192.168.0.1:8080|192.168.0.1:80|false|true",
        "[2001:db8::1]:8080|[2001:db8::1]:8080|true|true",
        "[2001:db8::1]:8080|[2001:db8::1]:80|false|true",
        "localhost:8080|https://localhost:8080/v3/api-doc|true|true",
        "zowe.github.io|https://zowe.github.io/docs-site/|true|false",
        "localhost:7553|https://:7553/|false|true",
        "localhost:7553|https://:7553/|false|false",
        "localhost:443|https://:443@/|false|true",
        "localhost:443|https://:443@/|false|false",
    })
    void givenAllowedList_whenIsAllowedDomain_thenDecide(String allowList, String domain, boolean isAllowed, boolean validatePort) {
        ReflectionTestUtils.setField(metadataValidator,"allowedDomainsSet", new HashSet<>(Arrays.asList(allowList.split(","))));
        assertEquals(isAllowed, metadataValidator.isAllowedDomain(domain, validatePort));
    }

}
