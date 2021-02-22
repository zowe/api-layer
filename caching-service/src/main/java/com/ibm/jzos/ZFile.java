/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ibm.jzos;

public class ZFile {
    private static int counter = 0;
    private static int counter2 = 0;

    public ZFile(String param1, String param2, int param3) {
        //Verify what happens if there is some Error thrown from this.
        if ( counter > 3 ) {
            //throw new ZFileException("", "", "", 0, 0, 0, null, 0, 0, 0, 0, 0);
        }

        counter++;
    }

    public void close() throws ZFileException, RcException {

    }

    public void delrec() throws ZFileException {

    }

    public boolean locate(byte[] key, int options) throws ZFileException {
        return false;
    }

    public boolean locate(byte[] key, int offset, int length, int options) throws ZFileException {
        return true;
    }

    public boolean locate(long recordNumberOrRBA, int options) throws ZFileException {
        return true;
    }

    public int read(byte[] buf) throws ZFileException {
        return -1;
    }

    public int read(byte[] buf, int offset, int len) throws ZFileException {
        return -1;
    }

    public int update(byte[] buf) throws ZFileException {
        return -1;
    }

    public int update(byte[] buf, int offset, int length) throws ZFileException {
        return -1;
    }

    public void write(byte[] buf) throws ZFileException {
        if (counter2 > 3) {
            throw new ZFileException("","","",0,0,0,null,0,0,0,0,0);
        }
        counter2++;
    }

    public void write(byte[] buf, int offset, int len) throws ZFileException {
        if (counter2 > 3) {
            throw new ZFileException("","","",0,0,0,null,0,0,0,0,0);
        }
        counter2++;
    }

    public String getActualFilename() {
        return null;
    }
}
