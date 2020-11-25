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
public class ZFileDummyImpl implements ZFile {

    public static final String NOT_IMPLEMENTED = "Not Implemented";

    public ZFileDummyImpl() {
        throw new UnsupportedOperationException("Dummy Implementation should not be instantiated");
    }

    @Override
    public void close() throws ZFileException, RcException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public void delrec() throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public boolean locate(byte[] key, int options) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public boolean locate(byte[] key, int offset, int length, int options) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public boolean locate(long recordNumberOrRBA, int options) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public int read(byte[] buf) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public int update(byte[] buf) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public int update(byte[] buf, int offset, int length) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public void write(byte[] buf) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public void write(byte[] buf, int offset, int len) throws ZFileException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public String getActualFilename() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
