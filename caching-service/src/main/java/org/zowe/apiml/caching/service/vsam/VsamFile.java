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
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;
import org.zowe.apiml.zfile.*;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ZFile wrapper providing convenience methods and implementing Closeable interface
 * This class is intended for serialized access to VSAM file.
 * Concurrency is to be handled by retrying.
 * Creates a proxy of com.ibm.jzos.ZFileException and provides high level methods for CRUD operations
 */

@Slf4j
public class VsamFile implements Closeable {

    private ZFile zfile;
    private VsamConfig vsamConfig;
    private final VsamConfig.VsamOptions options;

    public static final String VSAM_RECORD_ERROR_MESSAGE = "VsamRecordException occured: {}";
    public static final String RECORD_FOUND_MESSAGE = "Record found: {}";
    public static final String RECORD_CANNOT_BE_NULL_MESSAGE = "Record cannot be null";
    public static final String UNSUPPORTED_ENCODING_MESSAGE = "Unsupported encoding: {}";

    private static final Pattern REGEX_CORRECT_FILENAME = Pattern.compile("^\\/\\/\\'.*'");

    public VsamFile(VsamConfig config, VsamConfig.VsamOptions options) {
        this(config, options, false);
    }

    public VsamFile(VsamConfig config, VsamConfig.VsamOptions options, boolean performWarmup) {
        if (config == null) {
            throw new IllegalArgumentException("Cannot create VsamFile with null configuration");
        }

        this.vsamConfig = config;
        this.options = options;
        log.info("VsamFile::new with parameters: {}, Vsam options: {}", this.vsamConfig, this.options);

        if (!REGEX_CORRECT_FILENAME.matcher(vsamConfig.getFileName()).find()) {
            throw new IllegalArgumentException("VsamFile name does not conform to //'VSAM.DATASET.NAME' pattern");
        }

        try {
            this.zfile = openZfile();

            if (performWarmup) {
                log.info("Warming up VSAM file");
                warmUpVsamFile();
            }

        } catch (ZFileException | VsamRecordException e) {
            log.error("Problem initializing VSAM storage, opening of {} in mode {} has failed. Exception thrown: {}", vsamConfig, this.options, e);
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
     * <p>
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

    public Optional<VsamRecord> create(VsamRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(RECORD_CANNOT_BE_NULL_MESSAGE);
        }
        try {
            boolean found = zfile.locate(record.getKeyBytes(),
                ZFileConstants.LOCATE_KEY_EQ);

            if (!found) {
                log.info("Writing Record: {}", record);
                zfile.write(record.getBytes());
                return Optional.of(record);
            } else {
                log.error("The record already exists and will not be created. Use update instead.");
            }
        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        }

        return Optional.empty();
    }

    public Optional<VsamRecord> read(VsamRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(RECORD_CANNOT_BE_NULL_MESSAGE);
        }

        try {
            boolean found = zfile.locate(record.getKeyBytes(),
                ZFileConstants.LOCATE_KEY_EQ);
            log.info(RECORD_FOUND_MESSAGE, found);
            if (found) {
                byte[] recBuf = new byte[vsamConfig.getRecordLength()];
                zfile.read(recBuf);
                log.trace("RecBuf: {}", recBuf);
                log.info("ConvertedStringValue: {}", new String(recBuf, vsamConfig.getEncoding()));
                VsamRecord returned = new VsamRecord(vsamConfig, record.getServiceId(), recBuf);
                log.info("VsamRecord read: {}", returned);
                return Optional.of(returned);
            } else {
                log.error("No record found with requested key");
            }
        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (UnsupportedEncodingException e) {
            log.error(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        }
        return Optional.empty();
    }

    public Optional<VsamRecord> update(VsamRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(RECORD_CANNOT_BE_NULL_MESSAGE);
        }

        try {
            boolean found = zfile.locate(record.getKeyBytes(),
                ZFileConstants.LOCATE_KEY_EQ);

            log.info(RECORD_FOUND_MESSAGE, found);

            if (found) {
                byte[] recBuf = new byte[vsamConfig.getRecordLength()];
                zfile.read(recBuf); //has to be read before update/delete
                log.trace("Read found record: {}", new String(recBuf, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE));
                log.info("Will update record: {}", record);
                int nUpdated = zfile.update(record.getBytes());
                log.info("ZFile.update return value: {}", nUpdated);
                return Optional.of(record);
            } else {
                log.error("No record updated because no record found with key");
            }

        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (UnsupportedEncodingException e) {
            log.error(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        }

        return Optional.empty();
    }

    public Optional<VsamRecord> delete(VsamRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(RECORD_CANNOT_BE_NULL_MESSAGE);
        }

        try {
            boolean found = zfile.locate(record.getKeyBytes(),
                ZFileConstants.LOCATE_KEY_EQ);
            log.info(RECORD_FOUND_MESSAGE, found);

            if (found) {
                byte[] recBuf = new byte[vsamConfig.getRecordLength()];
                zfile.read(recBuf); //has to be read before update/delete
                VsamRecord returned = new VsamRecord(vsamConfig, record.getServiceId(), recBuf);
                zfile.delrec();
                log.info("Deleted vsam record: {}", returned);
                return Optional.of(returned);
            } else {
                log.error("No record deleted because no record found with key");
            }

        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        }

        return Optional.empty();
    }

    public List<VsamRecord> readForService(String serviceId) {
        if (serviceId == null || serviceId.isEmpty()) {
            throw new IllegalArgumentException("serviceId cannot be null");
        }
        List<VsamRecord> returned = new ArrayList<>();

        VsamKey key = new VsamKey(vsamConfig);

        boolean found;

        try {
            byte[] recBuf = new byte[vsamConfig.getRecordLength()];

            String keyGe = key.getKeySidOnly(serviceId);
            log.info("Attempt to find key in KEY_GE mode: {}", keyGe);

            found = zfile.locate(key.getKeyBytesSidOnly(serviceId),
                ZFileConstants.LOCATE_KEY_GE);

            log.info(RECORD_FOUND_MESSAGE, found);

            int overflowProtection = 10000;

            while (found) {
                int nread = zfile.read(recBuf);
                log.trace("RecBuf: {}", recBuf);
                log.info("nread: {}", nread);

                String convertedStringValue = new String(recBuf, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);

                VsamRecord record = new VsamRecord(vsamConfig, serviceId, recBuf);
                log.info("Read record: {}", record);

                if (nread < 0) {
                    log.info("nread is < 0, stopping the retrieval");
                    found = false;
                    continue;
                }

                log.trace("convertedStringValue: >{}<", convertedStringValue);
                log.trace("keyGe: >{}<", keyGe);
                if (!convertedStringValue.trim().startsWith(keyGe.trim())) {
                    log.info("read record does not start with serviceId's keyGe, stopping the retrieval");
                    found = false;
                    continue;
                } else {
                    log.info("read record starts with serviceId's keyGe, retrieving this record");
                }

                returned.add(record);

                overflowProtection--;
                if (overflowProtection <= 0) {
                    log.error("Maximum number of records retrieved, stopping the retrieval");
                    break;
                }
            }
        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (UnsupportedEncodingException e) {
            log.error(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        }

        return returned;
    }

    public List<String> readRecords() {
        byte[] ignoreKey = new byte[0];
        try {
            ignoreKey = " ".getBytes(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (UnsupportedEncodingException e) {
            log.error(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        }
        List<String> returned = new ArrayList<>();

//        boolean found;

        try {
            byte[] recBuf = new byte[vsamConfig.getRecordLength()];

//            found = zfile.locate(ignoreKey,
//                ZFileConstants.LOCATE_KEY_FIRST);

//            log.info(RECORD_FOUND_MESSAGE, found);

            int overflowProtection = 10000;
//            int nread = zfile.read(recBuf);
            while (zfile.read(recBuf) != -1) {

                log.trace("RecBuf: {}", recBuf);
//                log.info("nread: {}", nread);

                String record = new String(recBuf);
                log.info("Read record: {}", record);

                returned.add(record);

                overflowProtection--;
                if (overflowProtection <= 0) {
                    log.error("Maximum number of records retrieved, stopping the retrieval");
                    break;
                }
            }
        } catch (ZFileException e) {
            log.error(e.toString());
        }
        log.info("The Returned recordss: {}", returned);
        return returned;
    }

    @SuppressWarnings({"squid:S1130", "squid:S1192"})
    private ZFile openZfile() throws ZFileException {
        if (!REGEX_CORRECT_FILENAME.matcher(vsamConfig.getFileName()).find()) {
            throw new IllegalStateException("VsamFile does not exist");
        }
        return ClassOrDefaultProxyUtils.createProxyByConstructor(ZFile.class, "com.ibm.jzos.ZFile",
            ZFileDummyImpl::new,
            new Class[]{String.class, String.class, int.class},
            new Object[]{vsamConfig.getFileName(), options.getOptionsString(), ZFileConstants.FLAG_DISP_SHR + ZFileConstants.FLAG_PDS_ENQ},
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

}
