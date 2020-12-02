/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam;

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.caching.config.VsamConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;
import org.zowe.apiml.zfile.*;

import java.io.Closeable;
import java.util.regex.Pattern;

/**
 * ZFile wrapper providing convenience methods and implementing Closeable interface
 *
 * Creates a proxy of com.ibm.jzos.ZFileException and wraps it's methods
 *
 */

@Slf4j
public class VsamFile implements Closeable, ZFile {

    private ZFile zfile;
    private VsamConfig vsamConfig;
    private String options = "ab+,type=record";
    private static final Pattern REGEX_CORRECT_FILENAME = Pattern.compile("^\\/\\/\\'.*'");

    public VsamFile(VsamConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Cannot create VsamFile with null configuration");
        }
        this.vsamConfig = config;
        try {
            this.zfile = openZfile();
        } catch (ZFileException e) {
            throw new IllegalStateException("Failed to open VsamFile");
        }
    }

    @Override
    public void close() {

        if (zfile != null) {
            try {
                zfile.close();
            } catch (ZFileException e) {
                log.error("Closing ZFile failed");
            }
        }
    }

    /**
     * This method writes a record to file and deletes it immediately.
     * Use this method on freshly created empty VSAM to write the fist record
     * and to verify that records can be written.
     *
     * Exceptions are thrown to give chance to the caller to react.
     *
     * @throws ZFileException
     * @throws VsamRecordException
     */
    public void warmUpVsamFile() throws ZFileException, VsamRecordException {

        log.info("Warming up the vsam file by writing and deleting a record");

        log.info("VSAM file being used: {}", zfile.getActualFilename());

        VsamRecord record = new VsamRecord(vsamConfig, "delete", new KeyValue("me", "novalue"));

        log.info("Writing Record: {}", record);
        zfile.write(record.getBytes());

        boolean found = zfile.locate(record.getKeyBytes(), ZFileConstants.LOCATE_KEY_EQ);

        log.info("Test record for deletion found: {}", found);
        if (found) {
            byte[] recBuf = new byte[vsamConfig.getRecordLength()];
            zfile.read(recBuf); //has to be read before update/delete
            zfile.delrec();
            log.info("Test record deleted.");
        }

    }

    @SuppressWarnings({"squid:S1130", "squid:S1192"})
    private ZFile openZfile() throws ZFileException {
        if (!REGEX_CORRECT_FILENAME.matcher(vsamConfig.getFileName()).find()) {
            throw new IllegalStateException("VsamFile does not exist");
        }
        return ClassOrDefaultProxyUtils.createProxyByConstructor(ZFile.class, "com.ibm.jzos.ZFile",
            ZFileDummyImpl::new,
            new Class[] {String.class, String.class},
            new Object[] {vsamConfig.getFileName(), options},
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.jzos.ZFileException", ZFileException.class,
                "getFileName", "getMessage", "getErrnoMsg", "getErrno", "getErrno2", "getLastOp", "getAmrcBytes",
                "getAbendCode", "getAbendRc", "getFeedbackRc", "getFeedbackFtncd", "getFeedbackFdbk"),
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.jzos.RcException", RcException.class,
                "getMessage", "getRc"),
            new ClassOrDefaultProxyUtils.ByMethodName<>(
                "com.ibm.jzos.EnqueueException", EnqueueException.class,
                "getMessage", "getRc")
        );
    }

    @Override
    public void delrec() throws ZFileException {
        zfile.delrec();
    }

    @Override
    public boolean locate(byte[] key, int options) throws ZFileException {
        return zfile.locate(key, options);
    }

    @Override
    public boolean locate(byte[] key, int offset, int length, int options) throws ZFileException {
        return zfile.locate(key, offset, length, options);
    }

    @Override
    public boolean locate(long recordNumberOrRBA, int options) throws ZFileException {
        return zfile.locate(recordNumberOrRBA, options);
    }

    @Override
    public int read(byte[] buf) throws ZFileException {
        return zfile.read(buf);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws ZFileException {
        return zfile.read(buf, offset, len);
    }

    @Override
    public int update(byte[] buf) throws ZFileException {
        return zfile.update(buf);
    }

    @Override
    public int update(byte[] buf, int offset, int length) throws ZFileException {
        return zfile.update(buf, offset, length);
    }

    @Override
    public void write(byte[] buf) throws ZFileException {
        zfile.write(buf);
    }

    @Override
    public void write(byte[] buf, int offset, int len) throws ZFileException {
        zfile.write(buf, offset, len);
    }

    @Override
    public String getActualFilename() {
        return zfile.getActualFilename();
    }
}
