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

import lombok.experimental.UtilityClass;

@UtilityClass
public class ZFileConstants {
    public static final java.lang.String DEFAULT_EBCDIC_CODE_PAGE = "IBM-1047";
    public static final int DEVICE_DISK = 0;
    public static final int DEVICE_DUMMY = 6;
    public static final int DEVICE_HFS = 9;
    public static final int DEVICE_HIPERSPACE = 10;
    public static final int DEVICE_MEMORY = 8;
    public static final int DEVICE_MSGFILE = 7;
    public static final int DEVICE_OTHER = 255;
    public static final int DEVICE_PRINTER = 2;
    public static final int DEVICE_TAPE = 3;
    public static final int DEVICE_TDQ = 5;
    public static final int DEVICE_TERMINAL = 1;
    public static final int DSORG_CONCAT = 16;
    public static final int DSORG_HFS = 512;
    public static final int DSORG_HIPER = 64;
    public static final int DSORG_MEM = 32;
    public static final int DSORG_PDS_DIR = 5;
    public static final int DSORG_PDS_MEM = 3;
    public static final int DSORG_PDSE = 1024;
    public static final int DSORG_PO = 1;
    public static final int DSORG_PS = 8;
    public static final int DSORG_TEMP = 128;
    public static final int DSORG_VSAM = 256;
    public static final int ERRNO_E_ABEND = 92;
    public static final int ERRNO_E_DEFINEFILE = 61;
    public static final int ERRNO_E_READERR = 66;
    public static final int ERRNO_E_WRITEERR = 65;
    public static final int ERRNO_EACCES = 111;
    public static final int ERRNO_EILSEQ = 147;
    public static final int ERRNO_EINVAL = 121;
    public static final int ERRNO_EIO = 122;
    public static final int ERRNO_EPERM = 139;
    public static final int FLAG_DISP_MOD = 8;
    public static final int FLAG_DISP_OLD = 4;
    public static final int FLAG_DISP_SHR = 1;
    public static final int FLAG_PDS_ENQ = 2;
    public static final int LAST_OP_BSAM_BLDL = 8;
    public static final int LAST_OP_BSAM_CLOSE = 2;
    public static final int LAST_OP_BSAM_CLOSE_T = 7;
    public static final int LAST_OP_BSAM_NOTE = 4;
    public static final int LAST_OP_BSAM_OPEN = 1;
    public static final int LAST_OP_BSAM_POINT = 5;
    public static final int LAST_OP_BSAM_READ = 3;
    public static final int LAST_OP_BSAM_STOW = 9;
    public static final int LAST_OP_BSAM_WRITE = 6;
    public static final int LAST_OP_C_CANNOT_EXTEND = 66;
    public static final int LAST_OP_C_DBCS_SI_TRUNCATE = 64;
    public static final int LAST_OP_C_DBCS_SO_TRUNCATE = 63;
    public static final int LAST_OP_C_DBCS_TRUNCATE = 62;
    public static final int LAST_OP_C_DBCS_UNEVEN = 65;
    public static final int LAST_OP_C_FCBCHECK = 61;
    public static final int LAST_OP_C_TRUNCATE = 60;
    public static final int LAST_OP_HSP_CREATE = 301;
    public static final int LAST_OP_HSP_DELETE = 302;
    public static final int LAST_OP_HSP_EXTEND = 305;
    public static final int LAST_OP_HSP_READ = 303;
    public static final int LAST_OP_HSP_WRITE = 304;
    public static final int LAST_OP_IO_CATALOG = 45;
    public static final int LAST_OP_IO_DEVTYPE = 40;
    public static final int LAST_OP_IO_LOCATE = 44;
    public static final int LAST_OP_IO_OBTAIN = 43;
    public static final int LAST_OP_IO_RDJFCB = 41;
    public static final int LAST_OP_IO_RENAME = 47;
    public static final int LAST_OP_IO_SCRATCH = 48;
    public static final int LAST_OP_IO_SWAREQ = 49;
    public static final int LAST_OP_IO_TRKCALC = 42;
    public static final int LAST_OP_IO_UNCATALOG = 46;
    public static final int LAST_OP_QSAM_FREEPOOL = 154;
    public static final int LAST_OP_QSAM_GET = 150;
    public static final int LAST_OP_QSAM_PUT = 151;
    public static final int LAST_OP_QSAM_RELSE = 153;
    public static final int LAST_OP_QSAM_TRUNC = 152;
    public static final int LAST_OP_SVC99_ALLOC = 50;
    public static final int LAST_OP_SVC99_ALLOC_NEW = 51;
    public static final int LAST_OP_SVC99_UNALLOC = 52;
    public static final int LAST_OP_TGET_READ = 20;
    public static final int LAST_OP_TGET_WRITE = 21;
    public static final int LAST_OP_VSAM_CLOSE = 119;
    public static final int LAST_OP_VSAM_ENDREQ = 118;
    public static final int LAST_OP_VSAM_ERASE = 117;
    public static final int LAST_OP_VSAM_GENCB = 113;
    public static final int LAST_OP_VSAM_GET = 114;
    public static final int LAST_OP_VSAM_MODCB = 110;
    public static final int LAST_OP_VSAM_OPEN_ESDS = 101;
    public static final int LAST_OP_VSAM_OPEN_ESDS_PATH = 104;
    public static final int LAST_OP_VSAM_OPEN_FAIL = 100;
    public static final int LAST_OP_VSAM_OPEN_KSDS = 103;
    public static final int LAST_OP_VSAM_OPEN_KSDS_PATH = 105;
    public static final int LAST_OP_VSAM_OPEN_RRDS = 102;
    public static final int LAST_OP_VSAM_POINT = 116;
    public static final int LAST_OP_VSAM_PUT = 115;
    public static final int LAST_OP_VSAM_SHOWCB = 112;
    public static final int LAST_OP_VSAM_TESTCB = 111;
    public static final int LOCATE_KEY_EQ = 3;
    public static final int LOCATE_KEY_EQ_BWD = 4;
    public static final int LOCATE_KEY_FIRST = 1;
    public static final int LOCATE_KEY_GE = 5;
    public static final int LOCATE_KEY_LAST = 2;
    public static final int LOCATE_RBA_EQ = 0;
    public static final int LOCATE_RBA_EQ_BWD = 6;
    public static final int MODE_FLAG_APPEND = 4;
    public static final int MODE_FLAG_READ = 1;
    public static final int MODE_FLAG_UPDATE = 8;
    public static final int MODE_FLAG_WRITE = 2;
    public static final int OPEN_MODE_BINARY = 1;
    public static final int OPEN_MODE_RECORD = 2;
    public static final int OPEN_MODE_TEXT = 0;
    public static final int RECFM_A = 64;
    public static final int RECFM_B = 8;
    public static final int RECFM_F = 1;
    public static final int RECFM_M = 32;
    public static final int RECFM_S = 16;
    public static final int RECFM_U = 4;
    public static final int RECFM_V = 2;
    public static final int S_IRGRP = 32;
    public static final int S_IROTH = 4;
    public static final int S_IRUSR = 256;
    public static final int S_IRWXG = 56;
    public static final int S_IRWXO = 7;
    public static final int S_IRWXU = 448;
    public static final int S_ISGID = 1024;
    public static final int S_ISUID = 2048;
    public static final int S_ISVTX = 512;
    public static final int S_IWGRP = 16;
    public static final int S_IWOTH = 2;
    public static final int S_IWUSR = 128;
    public static final int S_IXGRP = 8;
    public static final int S_IXOTH = 1;
    public static final int S_IXUSR = 64;
    public static final int SEEK_CUR = 1;
    public static final int SEEK_END = 2;
    public static final int SEEK_SET = 0;
    public static final int VSAM_TYPE_ESDS = 1;
    public static final int VSAM_TYPE_ESDS_PATH = 4;
    public static final int VSAM_TYPE_KSDS = 2;
    public static final int VSAM_TYPE_KSDS_PATH = 5;
    public static final int VSAM_TYPE_NOTVSAM = 0;
    public static final int VSAM_TYPE_RRDS = 3;
}
