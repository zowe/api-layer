/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gzip;

import jakarta.servlet.http.HttpServletResponse;


public final class GZipResponseUtils {

    /**
     * Gzipping an empty file or stream always results in a 20 byte output
     * This is in java or elsewhere.
     * <p/>
     * On a unix system to reproduce do <code>gzip -n empty_file</code>. -n tells gzip to not
     * include the file name. The resulting file size is 20 bytes.
     * <p/>
     * Therefore 20 bytes can be used indicate that the gzip byte[] will be empty when ungzipped.
     */
    private static final int EMPTY_GZIPPED_CONTENT_SIZE = 20;

    /**
     * Utility class. No public constructor.
     */
    private GZipResponseUtils() {
    }

    /**
     * Checks whether a gzipped body is actually empty and should just be zero.
     * When the compressedBytes is {@link #EMPTY_GZIPPED_CONTENT_SIZE} it should be zero.
     *
     * @param compressedBytes the gzipped response body
     * @return true if the response should be 0, even if it is isn't.
     */
    public static boolean shouldGzippedBodyBeZero(byte[] compressedBytes) {
        return compressedBytes.length == EMPTY_GZIPPED_CONTENT_SIZE;
    }

    /**
     * Performs a number of checks to ensure response saneness according to the rules of RFC2616:
     * <ol>
     * <li>If the response code is {@link javax.servlet.http.HttpServletResponse#SC_NO_CONTENT} then it is forbidden for the body
     * to contain anything. See http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.5
     * <li>If the response code is {@link javax.servlet.http.HttpServletResponse#SC_NOT_MODIFIED} then it is forbidden for the body
     * to contain anything. See http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5
     * </ol>
     *
     * @param responseStatus the responseStatus
     * @return true if the response should be 0, even if it is isn't.
     */
    public static boolean shouldBodyBeZero(int responseStatus) {
        return responseStatus == HttpServletResponse.SC_NO_CONTENT || responseStatus == HttpServletResponse.SC_NOT_MODIFIED;
    }

    /**
     * Adds the gzip HTTP header to the response.
     * <p/>
     * <p>
     * This is need when a gzipped body is returned so that browsers can properly decompress it.
     * </p>
     *
     * @param response the response which will have a header added to it. I.e this method changes its parameter
     * @throws RuntimeException Either the response is committed or we were called using the include method
     *                          from a {@link javax.servlet.RequestDispatcher#include(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}
     *                          method and the set header is ignored.
     */
    public static void addGzipHeader(final HttpServletResponse response) throws GZipResponseException {
        response.setHeader("Content-Encoding", "gzip");
        boolean containsEncoding = response.containsHeader("Content-Encoding");
        if (!containsEncoding) {
            throw new GZipResponseException("Failure when attempting to set "
                + "Content-Encoding: gzip");
        }
    }
}
