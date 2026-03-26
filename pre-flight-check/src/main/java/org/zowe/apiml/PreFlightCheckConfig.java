/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

public interface PreFlightCheckConfig {

    String getZosmfHost();

    int getZosmfPort();

    String getScheme();

    String getKeyStore();

    String getKeyStorePassword();

    String getKeyStoreType();

    String getTrustStore();

    String getTrustStorePassword();

    String getTrustStoreType();

    boolean isHelpRequested();
}
