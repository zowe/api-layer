/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.HttpCookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class CookiesTest {

    BasicHeader contentLenght = new BasicHeader("content-length", "10");
    BasicHeader pragma = new BasicHeader("pragma", "once");
    BasicHeader cookieSingle = new BasicHeader("cookie", "cookie=cookie");
    BasicHeader cookieApiml = new BasicHeader("cookie", "apimlAuthenticationToken=eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJsdHBhIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE1ODczNzYzNDgsImV4cCI6MTU4NzQ2Mjc0OCwiaXNzIjoiQVBJTUwiLCJqdGkiOiJkNDRkMDY0ZS0yZmQ4LTQ1NzktYjZiYi02ZDRlZDkxN2UzZTQifQ.ehLKbbAvmUe7LhqSkFHVHAfvhofx1pyyzNVMv96BgJ068ma2jbv0mSfNo13mX8Ce2fJLjjkzrikbeyBWE1_uF6gv_s93ulhyOOOhpNm2aDkDBCMotM6TEFiD8WgMDbNPmhM70oB-ZuZbNmKCkQAAYHE481JObEnJLQ3KreUMb5AYnD-1Gl72AF40o3tfRjIRtZEwG41dvJaWFdhiUymW5epmgLV8ob8Cp3RV-MYl_cDh7z5rDxGPApzQ20Btqft_-toQQS1ZmdxDycKEUr-GkVfHc1gqWZUnigdNk5fC8KptNZbArvbeEsOxzOnDQCTKaWzhp0_f6M173vSnbQK7Iw;");
    BasicHeader cookieMultiple = new BasicHeader("cookie", "cookie1=cookie1;cookie2=cookie2;cookie3=cookie3");
    HttpRequest request;
    Cookies underTest;

    @BeforeEach
    public void setup() {
        request = mock(HttpRequest.class);
        underTest = Cookies.of(request);
    }

    @Test
    void givenRequestWithCookieHeader_whenGetAllCookies_thenRetrievesAllCookies() {
        doReturn(new Header[] {}).when(request).getHeaders(HttpHeaders.COOKIE);
        assertThat(underTest.getAll().size(), is(0));

        doReturn(new Header[] {cookieMultiple, cookieApiml}).when(request).getHeaders(HttpHeaders.COOKIE);
        HttpCookie testCookie = new HttpCookie("cookie1", "cookie1");
        testCookie.setVersion(0);
        assertThat(underTest.getAll().size(), is(4));
        assertThat(underTest.getAll(), hasItem(testCookie));
    }

    @Test
    void givenRequestWithCookieHeader_whenGetCookie_thenRetrievesCookies() {
        doReturn(new Header[] {cookieMultiple, cookieApiml}).when(request).getHeaders(HttpHeaders.COOKIE);
        HttpCookie testCookie = new HttpCookie("cookie1", "cookie1");
        assertThat(underTest.get("cookie1"), hasItem(testCookie));
    }

    @Test
    void givenRequestWithoutCookieHeader_whenSetCookie_thenCreatesCookieHeaderAndCookie() {
        doReturn(new Header[] {}).when(request).getHeaders(HttpHeaders.COOKIE);
        ArgumentCaptor<Header> argument = ArgumentCaptor.forClass(Header.class);
        HttpCookie newCookie = new HttpCookie("cookie1", "cookie1");
        newCookie.setVersion(0);

        underTest.set(newCookie);
        verify(request).setHeader(argument.capture());
        assertThat(argument.getValue().toString(), is("Cookie: cookie1=cookie1"));
    }

    @Test
    void givenRequestWithCookieHeader_whenSetCookie_thenCreatesOrOverwritesCookie() {
        doReturn(new Header[] { cookieSingle}).when(request).getHeaders(HttpHeaders.COOKIE);
        ArgumentCaptor<Header> argument = ArgumentCaptor.forClass(Header.class);

        HttpCookie newCookie = new HttpCookie("cookie1", "cookie1");
        newCookie.setVersion(0);

        underTest.set(newCookie);
        verify(request).setHeader(argument.capture());
        assertThat(argument.getValue().toString(), is("Cookie: cookie=cookie;cookie1=cookie1"));
    }

    @Test
    void givenRequestWithCookieHeaderWithSingleCookie_whenRemoveCookie_thenRemovesCookieAndHeader() {
        doReturn(new Header[] { cookieSingle}).when(request).getHeaders(HttpHeaders.COOKIE);

        underTest.remove("cookie");
        verify(request).removeHeaders(HttpHeaders.COOKIE);
    }

    @Test
    void givenRequestWithCookieHeaderWithMultipleCookies_whenRemoveCookie_thenRemovesCookie() {
        doReturn(new Header[] { cookieMultiple}).when(request).getHeaders(HttpHeaders.COOKIE);
        ArgumentCaptor<Header> argument = ArgumentCaptor.forClass(Header.class);

        underTest.remove("cookie1");
        verify(request).setHeader(argument.capture());
        assertThat(argument.getValue().toString(), is("Cookie: cookie2=cookie2;cookie3=cookie3"));
    }
}
