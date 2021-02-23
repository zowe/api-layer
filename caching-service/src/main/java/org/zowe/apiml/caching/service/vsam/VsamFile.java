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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.zfile.ZFile;
import org.zowe.apiml.zfile.ZFileConstants;
import org.zowe.apiml.zfile.ZFileException;

import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * ZFile wrapper providing convenience methods and implementing Closeable interface
 * This class is intended for serialized access to VSAM file.
 * Concurrency is to be handled by retrying.
 * Creates a proxy of com.ibm.jzos.ZFileException and provides high level methods for CRUD operations
 */

@Slf4j
public class VsamFile implements Closeable {

    @Getter
    private final ZFile zfile;
    private final VsamConfig vsamConfig;
    private final ZFileProducer zFileProducer;

    private final ApimlLogger apimlLog;

    public static final String VSAM_RECORD_ERROR_MESSAGE = "VsamRecordException occurred: {}";
    public static final String RECORD_FOUND_MESSAGE = "Record found: {}";
    public static final String RECORD_CANNOT_BE_NULL_MESSAGE = "Record cannot be null";
    public static final String UNSUPPORTED_ENCODING_MESSAGE = "Unsupported encoding: {}";

    private static final String ERROR_INITIALIZING_STORAGE_MESSAGE_KEY = "org.zowe.apiml.cache.errorInitializingStorage";
    private static final String STORAGE_TYPE = "VSAM";
    private static final Pattern REGEX_CORRECT_FILENAME = Pattern.compile("^\\/\\/\\'.*'");

    public VsamFile(VsamConfig config, VsamConfig.VsamOptions options, ApimlLogger apimlLogger) {
        this(config, options, false, apimlLogger);
    }

    public VsamFile(VsamConfig config, VsamConfig.VsamOptions options, boolean initialCreation, ApimlLogger apimlLogger) {
        this(config, options, initialCreation, new ZFileProducer(config, options, apimlLogger), new VsamInitializer(), apimlLogger);
    }

    public VsamFile(VsamConfig config, VsamConfig.VsamOptions options, boolean initialCreation, ZFileProducer zFileProducer, VsamInitializer vsamInitializer, ApimlLogger apimlLogger) {
        this.apimlLog = apimlLogger;
        if (config == null) {
            apimlLog.log(ERROR_INITIALIZING_STORAGE_MESSAGE_KEY, "vsam", "wrong Configuration", "No configuration provided");

            throw new IllegalArgumentException("Cannot create VsamFile with null configuration");
        }

        this.vsamConfig = config;
        log.info("VsamFile::new with parameters: {}, Vsam options: {}", this.vsamConfig, options);

        if (!REGEX_CORRECT_FILENAME.matcher(vsamConfig.getFileName()).find()) {
            String nonConformance = "VsamFile name does not conform to //'VSAM.DATASET.NAME' pattern";
            apimlLog.log(ERROR_INITIALIZING_STORAGE_MESSAGE_KEY, "vsam", "wrong Configuration", nonConformance);

            throw new IllegalArgumentException(nonConformance);
        }

        this.zFileProducer = zFileProducer;

        try {
            this.zfile = openZfile();

            if (initialCreation) {
                log.info("Warming up VSAM file");
                vsamInitializer.warmUpVsamFile(zfile, vsamConfig);
            }

        } catch (Exception e) {
            String info = String.format("opening of %s in mode %s failed", vsamConfig, options);
            if (initialCreation) {
                apimlLog.log(ERROR_INITIALIZING_STORAGE_MESSAGE_KEY, STORAGE_TYPE, info, e);
            } else {
                log.info(info);
            }

            throw new IllegalStateException("Failed to open VsamFile");
        }
    }

    @Override
    public void close() {
        if (zfile != null) {
            try {
                zfile.close();
            } catch (ZFileException e) {
                apimlLog.log("org.zowe.apiml.cache.errorQueryingStorage", STORAGE_TYPE, "Closing ZFile failed: " + e);
            }
        }
    }

    public Optional<VsamRecord> create(VsamRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(RECORD_CANNOT_BE_NULL_MESSAGE);
        }
        try {
            boolean found = zfile.locate(record.getKeyBytes(), ZFileConstants.LOCATE_KEY_EQ);

            if (!found) {
                log.info("Writing Record: {}", record);
                zfile.write(record.getBytes());
                return Optional.of(record);
            } else {
                log.info("The record already exists and will not be created. Use update instead.");
            }
        } catch (ZFileException e) {
            log.info(e.toString());

            throw new RetryableVsamException(e);
        } catch (VsamRecordException e) {
            log.info(VSAM_RECORD_ERROR_MESSAGE, e);

            throw new RetryableVsamException(e);
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
                VsamRecord returned = new VsamRecord(vsamConfig, recBuf);
                log.info("VsamRecord read: {}", returned);
                return Optional.of(returned);
            } else {
                log.info("No record found with requested key");
            }
        } catch (UnsupportedEncodingException e) {
            log.info(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (ZFileException e) {
            log.info(e.toString());

            throw new RetryableVsamException(e);
        } catch (VsamRecordException e) {
            log.info(VSAM_RECORD_ERROR_MESSAGE, e);

            throw new RetryableVsamException(e);
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
                log.info("No record updated because no record found with key");
            }

        } catch (UnsupportedEncodingException e) {
            log.info(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (ZFileException e) {
            log.info(e.toString());

            throw new RetryableVsamException(e);
        } catch (VsamRecordException e) {
            log.info(VSAM_RECORD_ERROR_MESSAGE, e);

            throw new RetryableVsamException(e);
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
                VsamRecord returned = new VsamRecord(vsamConfig, recBuf);
                zfile.delrec();
                log.info("Deleted vsam record: {}", returned);
                return Optional.of(returned);
            } else {
                log.info("No record deleted because no record found with key");
            }

        } catch (ZFileException e) {
            log.info(e.toString());

            throw new RetryableVsamException(e);
        } catch (VsamRecordException e) {
            log.info(VSAM_RECORD_ERROR_MESSAGE, e);

            throw new RetryableVsamException(e);
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

                VsamRecord record = new VsamRecord(vsamConfig, recBuf);
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
                    log.info("Maximum number of records retrieved, stopping the retrieval");
                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.info(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (ZFileException e) {
            log.info(e.toString());
        } catch (VsamRecordException e) {
            log.info(VSAM_RECORD_ERROR_MESSAGE, e);
        }

        return returned;
    }

    public Optional<byte[]> readBytes(byte[] arrayToStoreIn) throws ZFileException {
        if (getZfile().read(arrayToStoreIn) == -1) {
            return Optional.empty();
        }

        return Optional.of(arrayToStoreIn);
    }

    public Integer countAllRecords() {
        int recordsCounter = 0;

        try {
            byte[] recBuf = new byte[vsamConfig.getRecordLength()];

            int overflowProtection = 10000;
            while (zfile.read(recBuf) != -1) {

                log.trace("RecBuf: {}", recBuf);

                recordsCounter += 1;

                overflowProtection--;
                if (overflowProtection <= 0) {
                    log.info("Maximum number of records retrieved, stopping the retrieval");
                    break;
                }
            }
        } catch (ZFileException e) {
            log.info(e.toString());
        }
        return recordsCounter;
    }

    @SuppressWarnings({"squid:S1130", "squid:S1192"})
    private ZFile openZfile() throws VsamRecordException {
        return zFileProducer.openZfile();
    }

}
