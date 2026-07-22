/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit-level coverage of {@code NewSecurityConfiguration#buildHttpFirewall} and the
 * {@code apiml.security.enableStrictUrlValidation} option it consumes. It invokes the real production method
 * via reflection (the method is private on the inner {@code DefaultSecurity} configuration) so that the
 * selection logic itself is exercised:
 * <ul>
 *     <li>enabled (default) - a strict firewall rejects the controlled encoded characters on any path, both
 *     routed traffic and Gateway-internal endpoints;</li>
 *     <li>disabled - the routing-aware {@code ApimlStrictServerWebExchangeFirewall} forwards such characters on
 *     routed traffic, while Gateway-internal endpoints remain strictly validated.</li>
 * </ul>
 * The end-to-end wiring through the running Gateway is covered by
 * {@code org.zowe.apiml.acceptance.StrictUrlValidationTest}.
 */
class StrictUrlValidationFirewallTest {

    private static final String ROUTED_PATH = "/serviceid1/test/";
    private static final String INTERNAL_PATH = "/gateway/";

    /**
     * Every special character checked by strict validation.
     */
    static Stream<String> specialCharacters() {
        return Stream.concat(charactersRelaxedByOption(), Stream.of("encoded%2F%2Fslash"));
    }

    /**
     * The characters the Gateway relaxes for routed traffic when the option is disabled. Note that, unlike v3,
     * v2 does not relax the double-encoded slash ({@code %2F%2F}) - {@code buildHttpFirewall} does not call
     * {@code setAllowUrlEncodedDoubleSlash} - so it stays rejected even when validation is disabled.
     */
    static Stream<String> charactersRelaxedByOption() {
        return Stream.of(
            "encoded%2Fslash",
            "encoded%5Cbackslash",
            "encoded%25percent",
            "encoded%2Eperiod",
            "path;matrix"
        );
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("specialCharacters")
    void givenStrictValidationEnabled_thenRejectedOnRoutedAndInternalPaths(String pathSuffix) throws Exception {
        StrictHttpFirewall firewall = buildHttpFirewall(true);

        assertThrows(RequestRejectedException.class, () -> firewall.getFirewalledRequest(get(ROUTED_PATH + pathSuffix)));
        assertThrows(RequestRejectedException.class, () -> firewall.getFirewalledRequest(get(INTERNAL_PATH + pathSuffix)));
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("charactersRelaxedByOption")
    void givenStrictValidationDisabled_thenAllowedOnRoutedPath(String pathSuffix) throws Exception {
        StrictHttpFirewall firewall = buildHttpFirewall(false);

        assertDoesNotThrow(() -> firewall.getFirewalledRequest(get(ROUTED_PATH + pathSuffix)));
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("specialCharacters")
    void givenStrictValidationDisabled_thenStillRejectedOnInternalPath(String pathSuffix) throws Exception {
        StrictHttpFirewall firewall = buildHttpFirewall(false);

        assertThrows(RequestRejectedException.class, () -> firewall.getFirewalledRequest(get(INTERNAL_PATH + pathSuffix)));
    }

    private HttpServletRequest get(String uri) {
        return new MockHttpServletRequest(HttpMethod.GET.name(), uri);
    }

    // buildHttpFirewall is private on the deeply nested DefaultSecurity configuration
    // (NewSecurityConfiguration$AccessToken$DefaultSecurity), so it is invoked reflectively.
    private static final String CONFIG = NewSecurityConfiguration.class.getName();

    private StrictHttpFirewall buildHttpFirewall(boolean strictUrlValidationEnabled) throws Exception {
        NewSecurityConfiguration config = (NewSecurityConfiguration) newInstance(NewSecurityConfiguration.class, null);
        ReflectionTestUtils.setField(config, "isStrictUrlValidationEnabled", strictUrlValidationEnabled);

        Object accessToken = newInstance(Class.forName(CONFIG + "$AccessToken"), config);
        Object defaultSecurity = newInstance(Class.forName(CONFIG + "$AccessToken$DefaultSecurity"), accessToken);

        Method buildHttpFirewall = defaultSecurity.getClass().getDeclaredMethod("buildHttpFirewall");
        buildHttpFirewall.setAccessible(true);
        return (StrictHttpFirewall) buildHttpFirewall.invoke(defaultSecurity);
    }

    /**
     * Instantiates a class via its single declared constructor, supplying the enclosing instance (for inner
     * classes) as the first argument and {@code null} for every other dependency, which {@code buildHttpFirewall}
     * does not use.
     */
    private Object newInstance(Class<?> clazz, Object enclosingInstance) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object[] args = new Object[constructor.getParameterCount()];
        if (enclosingInstance != null && args.length > 0) {
            args[0] = enclosingInstance;
        }
        return constructor.newInstance(args);
    }

}
