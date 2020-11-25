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

import lombok.Getter;
import lombok.ToString;

import java.io.IOException;

@Getter
@ToString
@SuppressWarnings({"squid:S117", "squid:S116"}) //field names mirror zOS implementation
public class ZFileException extends IOException {

    private final String fileName;
    private final String msg;
    private final String errnoMsg;
    private final int errno;
    private final int errno2;
    private final int lastOp;
    private final byte[] amrc_code_bytes;
    private final int abendCode;
    private final int abendRc;
    private final int feedbackRc;
    private final int feedbackFtncd;
    private final int feedbackFdbk;


    @SuppressWarnings("squid:S107")
    public ZFileException(String fileName, String msg, String errnoMsg, int errno, int errno2, int lastOp, byte[] amrc_code_bytes,
                          int abendCode, int abendRc, int feedbackRc, int feedbackFtncd, int feedbackFdbk) {
        this.fileName = fileName;
        this.msg = msg;
        this.errnoMsg = errnoMsg;
        this.errno = errno;
        this.errno2 = errno2;
        this.lastOp = lastOp;
        this.amrc_code_bytes = amrc_code_bytes;
        this.abendCode = abendCode;
        this.abendRc = abendRc;
        this.feedbackRc = feedbackRc;
        this.feedbackFtncd = feedbackFtncd;
        this.feedbackFdbk = feedbackFdbk;
    }
}
