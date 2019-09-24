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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.function.Supplier;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.API_INFO;

@UtilityClass
public class UrlUtils {

    public static String trimSlashes(String string) {
        return string.replaceAll("^/|/$", "");
    }

    public static String createMetadataKey(String encodedUrl, String url) {
        return String.format("%s.%s.%s", API_INFO, encodedUrl, url);
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

}
