/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang.RandomStringUtils;

import java.net.*;
import java.security.InvalidParameterException;
import java.util.Enumeration;
import java.util.function.Supplier;


@UtilityClass
public class UrlUtils {

    public static String trimSlashes(String string) {
        return string.replaceAll("^/|/$", "");
    }

    public static String getEncodedUrl(String url) {
        if (url != null) {
            return url.replaceAll("\\W", "-");
        } else {
            return RandomStringUtils.randomAlphanumeric(10);
        }
    }

    public static void validateUrl(String url, Supplier<String> exceptionSupplier) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new InvalidParameterException(exceptionSupplier.get() + ": " + e.getMessage());
        }
    }

    public static String removeFirstAndLastSlash(String uri) {
        return StringUtils.removeFirstAndLastOccurrence(uri, "/");
    }

    public static String addFirstSlash(String uri) {
        return StringUtils.prependSubstring(uri, "/");
    }

    public static String removeLastSlash(String uri) {
        return StringUtils.removeLastOccurrence(uri, "/");
    }


    public String getBaseUrl() {
        String bU = null;
        String ownHost1 = getOwnHostFromInetAddress();
        if (ownHost1 != null) {
            bU = ownHost1;
        }
        String ownHost2 = getOwnHostFromDatagram();
        if (ownHost1 != null) {
            bU = ownHost1;
        }
        String ownHost3 = getOwnHostFromNetworkInterface();
        if (ownHost1 != null) {
            bU = ownHost1;
        }
        return bU;
    }

    private static  String getOwnHostFromNetworkInterface() {
        StringBuilder sb = new StringBuilder();

        try {
            Enumeration<NetworkInterface> allInterfaces =  NetworkInterface.getNetworkInterfaces();
            if (allInterfaces != null) {
                while (allInterfaces.hasMoreElements()) {
                    NetworkInterface ni = allInterfaces.nextElement();
                    if (ni.isPointToPoint()) {
                        sb.append(ni.toString());
                    }
                }

            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private static String getOwnHostFromDatagram() {
        StringBuilder sb = new StringBuilder();

        try (
            final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            sb.append(socket.getLocalAddress().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private static String getOwnHostFromInetAddress() {
        StringBuilder sb = new StringBuilder();
        try {
            InetAddress inetAddr = InetAddress.getLocalHost();
            sb.append(inetAddr.getHostAddress());
            //sb.append("          ip   is: ").append(inetAddr.getHostAddress());
            //sb.append(InetAddress.getLocalHost());
            //baseUrl = inetAddr.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
