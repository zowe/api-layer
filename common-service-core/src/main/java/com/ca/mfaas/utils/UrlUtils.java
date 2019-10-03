/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.function.Supplier;


@UtilityClass
public class UrlUtils {
    private static final Logger logger = LoggerFactory.getLogger(UrlUtils.class);

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

    private static  String getOwnIpFromNetworkInterface() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> allInterfaces =  NetworkInterface.getNetworkInterfaces();
            if (allInterfaces != null) {
                while (allInterfaces.hasMoreElements()) {
                    NetworkInterface ni = allInterfaces.nextElement();
                    if (ni.isPointToPoint()) {
                        ip = ni.toString();
                    }
                }

            }
        } catch (SocketException e) {
            logger.error("", e);
        }

        return ip;
    }

    private static String getOwnIpFromDatagram() {
        String ip = null;

        try (
            DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("", e);
        } catch (SocketException e) {
            logger.error("", e);
        }

        return ip;
    }

    private static String getOwnIpFromInetAddress() {
        String ip = null;
        try {
            InetAddress inetAddr = InetAddress.getLocalHost();
            ip = inetAddr.getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("", e);
        }
        return ip;
    }

    public static List<String> getHostBaseUrls() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objs = getHttpConnectorsNames(mbs, "HTTP/1.1", "Http11");

        ArrayList<String> endPoints = new ArrayList<>();
        if (objs != null) {
            InetAddress[] addresses = new InetAddress[0];
            try {
                InetAddress inetAddress = InetAddress.getLocalHost();
                addresses = InetAddress.getAllByName(inetAddress.getHostName());
            } catch (UnknownHostException e) {
                logger.error("", e);
            }

            for (ObjectName obj : objs ) {
                String scheme = "";
                try {
                    scheme = mbs.getAttribute(obj, "scheme").toString();
                } catch (AttributeNotFoundException e) {
                    logger.error("", e);
                } catch (InstanceNotFoundException e) {
                    logger.error("", e);
                } catch (ReflectionException e) {
                    logger.error("", e);
                } catch (MBeanException e) {
                    logger.error("", e);
                }

                String port = obj.getKeyProperty("port");
                for (InetAddress addr : addresses) {
                    if ((addr instanceof Inet6Address) || addr.isAnyLocalAddress() || addr.isLoopbackAddress() ||
                        addr.isMulticastAddress()) {
                        continue;
                    }
                    String host = addr.getHostAddress();
                    String ep = scheme + "://" + host + ":" + port;
                    endPoints.add(ep);
                }
            }
        }

        return endPoints;
    }

    private static Set<ObjectName> getHttpConnectorsNames(MBeanServer mbs, String protocol1, String protocol2) {
        QueryExp subQuery1 = Query.match(Query.attr("protocol"), Query.value(protocol1));
        QueryExp subQuery2 = Query.anySubString(Query.attr("protocol"), Query.value(protocol2));
        QueryExp query = Query.or(subQuery1, subQuery2);

        Set<ObjectName> objs = null;
        try {
            objs = mbs.queryNames(new ObjectName("*:type=Connector,*"), query);
        } catch (MalformedObjectNameException e) {
            logger.error("", e);
        }
        return objs;
    }
}
