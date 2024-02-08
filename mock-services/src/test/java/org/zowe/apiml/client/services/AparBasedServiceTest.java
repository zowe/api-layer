/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.client.services.apars.Apar;
import org.zowe.apiml.client.services.versions.Versions;

import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AparBasedServiceTest {
    private static final String BASE_VERSION = "2.4";
    private static final List<String> APPLIED_APARS = Collections.singletonList("PH12143");
    private static final String SERVICE = "service";
    private static final String METHOD = "method";
    private static final Map<String, String> HEADERS = new HashMap<>();

    private Apar apar;
    private MockHttpServletResponse response;
    private Versions versions;
    private AparBasedService underTest;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        apar = mock(Apar.class);
        versions = mock(Versions.class);
        underTest = new AparBasedService(BASE_VERSION, APPLIED_APARS, versions);
    }

    @Nested
    class whenProcessing {
        @Test
        void givenInvalidVersion_InternalServerErrorIsReturned() {
            ResponseEntity<?> expected = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            when(versions.fullSetOfApplied(any(), any())).thenThrow(new MockZosmfException("bad version"));

            ResponseEntity<?> result = underTest.process(SERVICE, METHOD, response, HEADERS);
            assertThat(result, is(expected));
        }

        @Test
        void givenOneAparVersion_returnResultOfApply() {
            Optional<ResponseEntity<?>> expectedResult = Optional.of(new ResponseEntity<>(HttpStatus.OK));
            ResponseEntity<?> expected = expectedResult.get();
            when(versions.fullSetOfApplied(any(), any())).thenReturn(Collections.singletonList(apar));
            when(apar.apply(any(Object[].class))).thenReturn(expectedResult);

            ResponseEntity<?> result = underTest.process(SERVICE, METHOD, response, HEADERS);
            assertThat(result, is(expected));
        }

        @Test
        void givenTwoAparVersions_returnLastResultOfApply() {
            Optional<ResponseEntity<?>> expectedResult = Optional.of(new ResponseEntity<>(HttpStatus.OK));
            ResponseEntity<?> expected = expectedResult.get();

            Apar apar2 = mock(Apar.class);
            List<Apar> aparList = new ArrayList<>();
            aparList.add(apar);
            aparList.add(apar2);
            when(versions.fullSetOfApplied(any(), any())).thenReturn(aparList);
            when(apar.apply(any(Object[].class))).thenReturn(Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT)));
            when(apar2.apply(any(Object[].class))).thenReturn(expectedResult);

            ResponseEntity<?> result = underTest.process(SERVICE, METHOD, response, HEADERS);
            assertThat(result, is(expected));
        }

        @Test
        void givenNoAparVersions_returnInternalServerError() {
            ResponseEntity<?> expected = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

            when(versions.fullSetOfApplied(any(), any())).thenReturn(Collections.emptyList());

            ResponseEntity<?> result = underTest.process(SERVICE, METHOD, response, HEADERS);
            assertThat(result, is(expected));
        }

        @Test
        void givenAparsThatReturnLastResult_returnInternalServerError() {
            ResponseEntity<?> expected = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

            when(versions.fullSetOfApplied(any(), any())).thenReturn(Collections.singletonList(apar));
            when(apar.apply(ArgumentMatchers.<Object>any())).thenReturn(Optional.empty());

            ResponseEntity<?> result = underTest.process(SERVICE, METHOD, response, HEADERS);
            assertThat(result, is(expected));
        }
    }
}
