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
 * Expose convenient methods to work with Cookies.
 *
 * As of HTTP/2 it is possible to have Cookies in multiple headers with the same name.
 */
public final class Cookies {
    private final HttpRequest request;

    private Cookies(HttpRequest request) {
        this.request = request;
    }

    public static Cookies of(HttpRequest request) {
        return new Cookies(request);
    }

    public List<HttpCookie> getAll() {
        List<HttpCookie> cookieList = new ArrayList<>();
        List<Header> cookieHeaders = getHeader(HttpHeaders.COOKIE);
        cookieHeaders.forEach(h -> cookieList.addAll(getAllCookiesFromHeader(h)));
        return cookieList;
    }

    public List<HttpCookie> get(String needle) {
        return getAll()
            .stream()
            .filter(c -> c.getName().equalsIgnoreCase(needle))
            .collect(Collectors.toList());
    }

    public void set(HttpCookie cookie) {
        if (getHeader(HttpHeaders.COOKIE).isEmpty()) {
            request.setHeader(new BasicHeader(HttpHeaders.COOKIE, cookie.toString()));
        } else {
            //cookie always added to first cookie header found
            Header cookieHeader = getHeader(HttpHeaders.COOKIE).get(0);
            List<HttpCookie> cookieList = getAllCookiesFromHeader(cookieHeader);
            cookieList = cookieList.stream()
                .filter(c -> !c.getName().equalsIgnoreCase(cookie.getName()))
                .collect(Collectors.toList());
            cookieList.add(cookie);
            request.setHeader(getCookieHeader(cookieList));
        }
    }

    public void remove(String cookie) {
        List<Header> cookieHeaders = getHeader(HttpHeaders.COOKIE);
        for (Header header: cookieHeaders) {
            List<HttpCookie> cookieList = getAllCookiesFromHeader(header);
            cookieList = cookieList.stream()
                .filter(c -> !c.getName().equalsIgnoreCase(cookie))
                .collect(Collectors.toList());
            if (cookieList.isEmpty()) {
                request.removeHeaders(HttpHeaders.COOKIE);
            } else {
                request.setHeader(getCookieHeader(cookieList));
            }
        }
    }

    private List<HttpCookie> getAllCookiesFromHeader(Header header) {
        String headerValue = header.getValue();
        if (headerValue == null || headerValue.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpCookie> cookieList = new ArrayList<>();
        List<String> cookieStringList = Arrays.asList(headerValue.split(";"));
        cookieStringList.forEach(s -> cookieList.addAll(HttpCookie.parse(s)));
        return cookieList;
    }

    private Header getCookieHeader(List<HttpCookie> cookieList) {
        return new BasicHeader(HttpHeaders.COOKIE, cookieList.stream()
            .map(HttpCookie::toString)
            .collect(Collectors.joining(";")));
    }

    private List<Header> getHeader(String needle) {
        return Arrays.asList(request.getHeaders(needle));
    }
}
