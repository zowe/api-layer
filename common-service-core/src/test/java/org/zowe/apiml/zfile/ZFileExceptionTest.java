package org.zowe.apiml.zfile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZFileExceptionTest {
    @Test
    void thenReturnMessage() {
        String message = "This is an error message";
        ZFileException exception = new ZFileException("file", "message", "error", 0, 0, 0, new byte[]{}, 0, 0, 0, 0, 0);
        assertEquals("file", exception.getFileName());
        assertEquals("message", exception.getMsg());
        assertEquals("error", exception.getErrnoMsg());
        assertEquals(0, exception.getErrno());
        assertEquals(0, exception.getAbendCode());
        assertEquals(0, exception.getAbendRc());
        assertEquals(0, exception.getAmrc_code_bytes().length);
        assertEquals(0, exception.getErrno2());
        assertEquals(0, exception.getFeedbackRc());
        assertEquals(0, exception.getFeedbackFdbk());
        assertEquals(0, exception.getFeedbackFtncd());
        assertEquals(0, exception.getLastOp());
    }
}
