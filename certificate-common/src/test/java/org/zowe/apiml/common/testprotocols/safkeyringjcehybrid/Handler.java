/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.common.testprotocols.safkeyringjcehybrid;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Stand-in for the IBM {@code safkeyringjcehybrid://} handler, found through {@code java.protocol.handler.pkgs}.
 * Loaded by java.net.URL via reflection, hence incorrectly reported as unused.
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) {
        return new URLConnection(url) {
            @Override
            public void connect() {
                // nothing to connect to
            }
        };
    }

}
