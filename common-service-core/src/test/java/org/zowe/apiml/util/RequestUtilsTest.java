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

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.HttpCookie;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

class RequestUtilsTest {

    BasicHeader contentLenght = new BasicHeader("content-length", "10");
    BasicHeader pragma = new BasicHeader("pragma", "once");
    BasicHeader cookieSingle = new BasicHeader("cookie", "cookie=cookie");
    BasicHeader cookieApiml = new BasicHeader("cookie", "apimlAuthenticationToken=eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJsdHBhIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE1ODczNzYzNDgsImV4cCI6MTU4NzQ2Mjc0OCwiaXNzIjoiQVBJTUwiLCJqdGkiOiJkNDRkMDY0ZS0yZmQ4LTQ1NzktYjZiYi02ZDRlZDkxN2UzZTQifQ.ehLKbbAvmUe7LhqSkFHVHAfvhofx1pyyzNVMv96BgJ068ma2jbv0mSfNo13mX8Ce2fJLjjkzrikbeyBWE1_uF6gv_s93ulhyOOOhpNm2aDkDBCMotM6TEFiD8WgMDbNPmhM70oB-ZuZbNmKCkQAAYHE481JObEnJLQ3KreUMb5AYnD-1Gl72AF40o3tfRjIRtZEwG41dvJaWFdhiUymW5epmgLV8ob8Cp3RV-MYl_cDh7z5rDxGPApzQ20Btqft_-toQQS1ZmdxDycKEUr-GkVfHc1gqWZUnigdNk5fC8KptNZbArvbeEsOxzOnDQCTKaWzhp0_f6M173vSnbQK7Iw;");
    BasicHeader cookieMultiple = new BasicHeader("cookie", "cookie1=cookie1;cookie2=cookie2;cookie3=cookie3");
    HttpRequest request;
    RequestUtils wrapper;

    @BeforeEach
    public void setup() {
        request = mock(HttpRequest.class);
        wrapper = RequestUtils.of(request);
    }

    @Test
    void givenRequestWithoutHeaders_whenGetHeaders_thenGetEmptyList() {
        doReturn(new Header[0]).when(request).getAllHeaders();
        assertThat(wrapper.getHeaders().size(), is(0));
    }

    @Test
    void givenRequestWithHeaders_whenGetHeaders_thenGetListWithHeaders() {
        doReturn(new Header[] {contentLenght, pragma}).when(request).getAllHeaders();
        assertThat(wrapper.getHeaders().size(), is(2));
        assertThat(wrapper.getHeaders(), hasItems(contentLenght, pragma));
    }

    @Test
    void givenRequestWithHeaders_whenGetHeader_thenGetHeaderOrEmpty() {
        doReturn(new Header[] {contentLenght, pragma}).when(request).getAllHeaders();
        assertThat(wrapper.getHeader("fishnets"), is(empty()));
        assertThat(wrapper.getHeader("pragma"), hasItem(pragma));
    }

    @Test
    public void givenRequestWithHeaders_whenSetExistingHeader_thenHeaderOverwritten() {
        doReturn(new Header[] { contentLenght, pragma}).when(request).getAllHeaders();
        ArgumentCaptor<Header[]> argument = ArgumentCaptor.forClass(Header[].class);

        BasicHeader modifiedHeader = new BasicHeader("pragma", "always");
        wrapper.setHeader(modifiedHeader);
        verify(request).setHeaders(argument.capture());
        assertThat(argument.getValue(), hasItemInArray(contentLenght));
        assertThat(argument.getValue(), hasItemInArray(modifiedHeader));
    }

    @Test
    void givenRequestWithHeaders_whenSetNewHeader_thenHeaderCreated() {
        doReturn(new Header[] { contentLenght, pragma}).when(request).getAllHeaders();
        ArgumentCaptor<Header[]> argument = ArgumentCaptor.forClass(Header[].class);

        BasicHeader newHeader = new BasicHeader("fishnets", "mipiace");
        wrapper.setHeader(newHeader);
        verify(request).setHeaders(argument.capture());
        assertThat(argument.getValue(), hasItemInArray(contentLenght));
        assertThat(argument.getValue(), hasItemInArray(pragma));
        assertThat(argument.getValue(), hasItemInArray(newHeader));
    }

    @Test
    void givenRequestWithHeaders_whenRemoveHeader_thenHeaderRemoved() {
        doReturn(new Header[] { contentLenght, pragma}).when(request).getAllHeaders();
        ArgumentCaptor<Header[]> argument = ArgumentCaptor.forClass(Header[].class);

        wrapper.removeHeader("pragma");
        verify(request).setHeaders(argument.capture());
        assertThat(argument.getValue(), hasItemInArray(contentLenght));
        assertThat(argument.getValue().length, is(1));
    }

    @Test
    void givenRequestWithCookieHeader_whenGetAllCookies_thenRetrievesAllCookies() {
        doReturn(new Header[] { contentLenght, pragma}).when(request).getAllHeaders();
        assertThat(wrapper.getAllCookies().size(), is(0));

        doReturn(new Header[] { contentLenght, pragma, cookieMultiple, cookieApiml}).when(request).getAllHeaders();
        HttpCookie testCookie = new HttpCookie("cookie1", "cookie1");
        testCookie.setVersion(0);
        assertThat(wrapper.getAllCookies().size(), is(4));
        assertThat(wrapper.getAllCookies(), hasItem(testCookie));
    }

    @Test
    void givenRequestWithCookieHeader_whenGetCookie_thenRetrievesCookies() {
        doReturn(new Header[] { contentLenght, pragma, cookieMultiple, cookieApiml}).when(request).getAllHeaders();
        HttpCookie testCookie = new HttpCookie("cookie1", "cookie1");
        assertThat(wrapper.getCookie("cookie1"), hasItem(testCookie));
    }

    @Test
    void givenRequestWithoutCookieHeader_whenSetCookie_thenCreatesCookieHeaderAndCookie() {
        doReturn(new Header[] { contentLenght, pragma}).when(request).getAllHeaders();
        ArgumentCaptor<Header[]> argument = ArgumentCaptor.forClass(Header[].class);
        HttpCookie newCookie = new HttpCookie("cookie1", "cookie1");
        newCookie.setVersion(0);

        wrapper.setCookie(newCookie);
        verify(request).setHeaders(argument.capture());
        assertThat(argument.getValue().length, is(3));
        assertThat(Arrays.asList(argument.getValue()), hasItem(hasToString("Cookie: cookie1=cookie1")));
    }

    @Test
    void givenRequestWithCookieHeader_whenSetCookie_thenCreatesOrOverwritesCookie() {
        doReturn(new Header[] { contentLenght, pragma, cookieSingle}).when(request).getAllHeaders();
        ArgumentCaptor<Header[]> argument = ArgumentCaptor.forClass(Header[].class);

        HttpCookie newCookie = new HttpCookie("cookie1", "cookie1");
        newCookie.setVersion(0);

        wrapper.setCookie(newCookie);
        verify(request).setHeaders(argument.capture());
        assertThat(argument.getValue().length, is(3));
        assertThat(Arrays.asList(argument.getValue()), hasItem(hasToString("Cookie: cookie=cookie;cookie1=cookie1")));
    }

    @Test
    void givenRequestWithCookieHeaderWithSingleCookie_whenRemoveCookie_thenRemovesCookieAndHeader() {
        doReturn(new Header[] { contentLenght, pragma, cookieSingle}).when(request).getAllHeaders();
        ArgumentCaptor<Header[]> argument = ArgumentCaptor.forClass(Header[].class);

        wrapper.removeCookie("cookie");
        verify(request).setHeaders(argument.capture());
        assertThat(argument.getValue().length, is(2));
        assertThat(argument.getValue(), hasItemInArray(contentLenght));
        assertThat(argument.getValue(), hasItemInArray(pragma));
    }

    @Test
    void givenRequestWithCookieHeaderWithMultipleCookies_whenRemoveCookie_thenRemovesCookie() {
        doReturn(new Header[] { contentLenght, pragma, cookieMultiple}).when(request).getAllHeaders();
        ArgumentCaptor<Header[]> argument = ArgumentCaptor.forClass(Header[].class);

        wrapper.removeCookie("cookie1");
        verify(request).setHeaders(argument.capture());
        assertThat(argument.getValue().length, is(3));
        assertThat(argument.getValue(), hasItemInArray(contentLenght));
        assertThat(argument.getValue(), hasItemInArray(pragma));
        assertThat(Arrays.asList(argument.getValue()), hasItem(hasToString("Cookie: cookie2=cookie2;cookie3=cookie3")));
    }
}
