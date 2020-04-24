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

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is meant for wrapping HttpRequest and exposing convenience mehtods
 * for working with headers and cookies
 *
 * Support for duplicate headers with same name is not present and can be added in future
 */
public class RequestUtils {

    private final HttpRequest request;

    private RequestUtils(HttpRequest request) {
        this.request = request;
    }

    public static RequestUtils of(HttpRequest request) {
        return new RequestUtils(request);
    }

    public List<Header> getHeaders() {
        return getAllHeaders();
    }

    private List<Header> getAllHeaders() {
        return new ArrayList<>(Arrays.asList(request.getAllHeaders()));
    }

    public List<Header> getHeader(String needle) {

        return getAllHeaders().stream()
            .filter(h -> h.getName().equalsIgnoreCase(needle))
            .collect(Collectors.toList());
    }

    public void setHeader(Header newHeader) {
        List<Header> newHeaderList = getAllHeaders().stream()
            .filter(h -> !h.getName().equalsIgnoreCase(newHeader.getName()))
            .collect(Collectors.toList());
        newHeaderList.add(newHeader);
        request.setHeaders(newHeaderList.toArray(new Header[] {}));
    }

    public void removeHeader(String needle) {
        List<Header> newHeaderList = getAllHeaders().stream()
            .filter(h -> !h.getName().equalsIgnoreCase(needle))
            .collect(Collectors.toList());
        request.setHeaders(newHeaderList.toArray(new Header[] {}));
    }

    public List<HttpCookie> getAllCookies() {
        List<HttpCookie> cookieList = new ArrayList<>();
        List<Header> cookieHeaders = getAllHeaders().stream()
            .filter(h -> h.getName().equalsIgnoreCase(HttpHeaders.COOKIE)).collect(Collectors.toList());

        cookieHeaders.forEach(h -> cookieList.addAll(getAllCookiesFromHeader(h)));
        return cookieList;
    }

    private List<HttpCookie> getAllCookiesFromHeader(Header header) {
        if (!header.getName().equalsIgnoreCase(HttpHeaders.COOKIE)) {
            throw new IllegalArgumentException("argument is not a cookie header");
        }
        String headerValue = header.getValue();
        if (headerValue == null || headerValue.isEmpty()) {
            return new ArrayList<>();
        } else {
            List<HttpCookie> cookieList = new ArrayList<>();
            List<String> cookieStringList = Arrays.asList(header.getValue().split(";"));
            cookieStringList.forEach(s -> cookieList.addAll(HttpCookie.parse(s)));
            return cookieList;
        }
    }

    public List<HttpCookie> getCookie(String needle) {
        return getAllCookies().stream().filter(c -> c.getName().equalsIgnoreCase(needle)).collect(Collectors.toList());
    }

    public void setCookie(HttpCookie cookie) {
        if (getHeader(HttpHeaders.COOKIE).isEmpty()) {
            setHeader(new BasicHeader(HttpHeaders.COOKIE, cookie.toString()));
        } else {
            //cookie always added to first cookie header found
            Header cookieHeader = getHeader(HttpHeaders.COOKIE).get(0);
            List<HttpCookie> cookieList = getAllCookiesFromHeader(cookieHeader);
            cookieList = cookieList.stream()
                .filter(c -> !c.getName().equalsIgnoreCase(cookie.getName()))
                .collect(Collectors.toList());
            cookieList.add(cookie);
            setHeader(getCookieHeader(cookieList));
        }
    }

    private Header getCookieHeader(List<HttpCookie> cookieList) {
        return new BasicHeader(HttpHeaders.COOKIE, cookieList.stream()
            .map(HttpCookie::toString)
            .collect(Collectors.joining(";")));
    }

    public void removeCookie(String cookie) {
        List<Header> cookieHeaders = getHeader(HttpHeaders.COOKIE);
        for (Header header: cookieHeaders) {
            List<HttpCookie> cookieList = getAllCookiesFromHeader(header);
            cookieList = cookieList.stream()
                .filter(c -> !c.getName().equalsIgnoreCase(cookie))
                .collect(Collectors.toList());
            if (cookieList.isEmpty()) {
                removeHeader(HttpHeaders.COOKIE);
            } else {
                setHeader(getCookieHeader(cookieList));
            }
        }
    }
}
