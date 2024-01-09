/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.mapping;

import org.zowe.commons.usermap.CertificateResponse;
import org.zowe.commons.usermap.MapperResponse;

/**
 * Wrapper interface around the <a href="https://github.com/zowe/common-java/blob/v2.x.x/zos-utils/src/main/java/org/zowe/commons/usermap/UserMapper.java">UserMapper</a> class
 * in the zos-utils library. It wraps public native methods for better testability.
 */
public interface NativeMapperWrapper {

    CertificateResponse getUserIDForCertificate(byte[] cert);

    MapperResponse getUserIDForDN(String dn, String registry);
}
