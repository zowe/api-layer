/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zfile;
@SuppressWarnings("squid:S1130")
public interface ZFile {

    void close() throws ZFileException, RcException;

    void delrec() throws ZFileException;

    boolean locate(byte[] key, int options) throws ZFileException;

    boolean locate(byte[] key, int offset, int length, int options) throws ZFileException;

    boolean locate(long recordNumberOrRBA, int options) throws ZFileException;

    int read(byte[] buf) throws ZFileException;

    int read(byte[] buf, int offset, int len) throws ZFileException;

    int update(byte[] buf) throws ZFileException;

    int update(byte[] buf, int offset, int length) throws ZFileException;

    void write(byte[] buf) throws ZFileException;

    void write(byte[] buf, int offset, int len) throws ZFileException;

    String getActualFilename();

}
