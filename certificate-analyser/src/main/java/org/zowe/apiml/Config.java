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

public interface Config {
    String getKeyStore();

    String getTrustStore();

    String getTrustPasswd();

    String getKeyPasswd();

    String getTrustStoreType();

    String getKeyStoreType();

    String getKeyAlias();

    String getRemoteUrl();

    boolean isHelpRequested();

    boolean isDoLocalHandshake();

    boolean isClientCertAuth();
}
