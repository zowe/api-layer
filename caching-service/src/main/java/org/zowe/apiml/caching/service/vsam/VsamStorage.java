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
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;
import org.zowe.apiml.util.ObjectUtil;
import org.zowe.apiml.zfile.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class VsamStorage implements Storage {

    public static final int RC_INVALID_VSAM_FILE = 1;
    public static final String VSAM_RECORD_ERROR_MESSAGE = "VsamRecordException occured: {}";
    public static final String RECORD_FOUND_MESSAGE = "Record found: {}";
    public static final String UNSUPPORTED_ENCODING_MESSAGE = "Unsupported encoding: {}";

    String options = "ab+,type=record";

    VsamConfig vsamConfig;
    VsamKey key;
    int keyLen;
    int lrecl;


    public VsamStorage(VsamConfig config, boolean isTestScope) {

        log.info("Using VSAM storage for the cached data");
        ObjectUtil.requireNotNull(config.getFileName(), "Vsam filename cannot be null");
        ObjectUtil.requireNotEmpty(config.getFileName(), "Vsam filename cannot be empty");


        this.vsamConfig = config;
        this.key = new VsamKey(config);
        this.keyLen = vsamConfig.getKeyLength();
        this.lrecl = vsamConfig.getRecordLength();

        log.info("Using Vsam configuration: {}", vsamConfig);

        if (!isTestScope) {
            warmUpVsamFile();
        }
    }

    public void warmUpVsamFile() {
        ZFile zfile = null;
        try {
            log.info("Warming up the vsam file by writing and deleting a record");
            zfile = openZfile();
            log.info("VSAM file being used: {}", zfile.getActualFilename());

            VsamRecord record = new VsamRecord(vsamConfig, "delete", new KeyValue("me", "novalue"));

            log.info("Writing Record: {}", record);
            zfile.write(record.getBytes());

            boolean found = zfile.locate(record.getKeyBytes(), ZFileConstants.LOCATE_KEY_EQ);

            log.info("Test record for deletion found: {}", found);
            if (found) {
                byte[] recBuf = new byte[lrecl];
                zfile.read(recBuf); //has to be read before update/delete
                zfile.delrec();
                log.info("Test record deleted.");
            }
        } catch (ZFileException | RcException e) {
            log.error("Problem initializing VSAM storage, opening of {} in mode {} has failed", vsamConfig, options);
            log.error(e.toString());
            System.exit(RC_INVALID_VSAM_FILE);
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        } finally {
            if (zfile != null) {
                closeZfile(zfile);
            }
        }
    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        log.info("Writing record: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());
        KeyValue result = null;
        ZFile zfile = null;
        try {
            zfile = openZfile();

            VsamRecord record = new VsamRecord(vsamConfig, serviceId, toCreate);

            boolean found = zfile.locate(record.getKeyBytes(),
                ZFileConstants.LOCATE_KEY_EQ);

            if (!found) {
                log.info("Writing Record: {}", record);
                zfile.write(record.getBytes());
                result = toCreate;
            } else {
                log.error("The record already exists and will not be created. Use update instead.");
            }


        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        } finally {
            closeZfile(zfile);
        }

        return result;
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        log.info("Reading Record: {}|{}|{}", serviceId, key, "-");
        KeyValue result = null;
        ZFile zfile = null;
        try {
            zfile = openZfile();
            boolean found = zfile.locate(this.key.getKeyBytes(serviceId, key),
                ZFileConstants.LOCATE_KEY_EQ);
            log.info(RECORD_FOUND_MESSAGE, found);
            if (found) {
                byte[] recBuf = new byte[lrecl];
                zfile.read(recBuf);
                log.info("RecBuf: {}", recBuf);
                log.info("ConvertedStringValue: {}", new String(recBuf, vsamConfig.getEncoding()));
                VsamRecord record = new VsamRecord(vsamConfig, serviceId, recBuf);
                log.info("VsamRecord read: {}", record);
                result = record.getKeyValue();
            }
        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (UnsupportedEncodingException e) {
            log.error(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        } finally {
            closeZfile(zfile);
        }
        return result;
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        log.info("Updating Record: {}|{}|{}", serviceId, toUpdate.getKey(), toUpdate.getValue());
        KeyValue result = null;
        ZFile zfile = null;
        try {
            zfile = openZfile();

            boolean found = zfile.locate(key.getKeyBytes(serviceId, toUpdate.getKey()),
                ZFileConstants.LOCATE_KEY_EQ);

            log.info(RECORD_FOUND_MESSAGE, found);

            if (found) {
                byte[] recBuf = new byte[lrecl];
                zfile.read(recBuf); //has to be read before update/delete
                log.info("Read found record: {}", new String(recBuf, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE));
                VsamRecord record = new VsamRecord(vsamConfig, serviceId, toUpdate);
                log.info("Construct updated record: {}", record);
                int nUpdated = zfile.update(record.getBytes());
                log.info("ZFile.update return value: {}", nUpdated);
                result = record.getKeyValue();
            } else {
                log.error("No record updated because no record found with key");
            }

        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (UnsupportedEncodingException e) {
            log.error(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        } finally {
            closeZfile(zfile);
        }

        return result;
    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {

        log.info("Deleting Record: {}|{}|{}", serviceId, toDelete, "-");
        KeyValue result = null;
        ZFile zfile = null;

        try {
            zfile = openZfile();

            boolean found = zfile.locate(key.getKeyBytes(serviceId, toDelete),
                ZFileConstants.LOCATE_KEY_EQ);
            log.info(RECORD_FOUND_MESSAGE, found);

            if (found) {
                byte[] recBuf = new byte[lrecl];
                zfile.read(recBuf); //has to be read before update/delete
                VsamRecord record = new VsamRecord(vsamConfig, serviceId, recBuf);
                zfile.delrec();
                log.info("Deleted vsam record: {}", record);
                result = record.getKeyValue();
            } else {
                log.error("No record deleted because no record found with key");
            }

        } catch (ZFileException e) {
            log.error(e.toString());
        } catch (UnsupportedEncodingException e) {
            log.error(UNSUPPORTED_ENCODING_MESSAGE, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        } catch (VsamRecordException e) {
            log.error(VSAM_RECORD_ERROR_MESSAGE, e);
        } finally {
            closeZfile(zfile);
        }

        return result;
    }

    @Override
    @SuppressWarnings("squid:S135")
    public Map<String, KeyValue> readForService(String serviceId) {

        log.info("Reading All Records: {}|{}|{}", serviceId, "-", "-");
        Map<String, KeyValue> result = new HashMap<>();
        ZFile zfile = null;
        try {
            zfile = openZfile();
            byte[] recBuf = new byte[lrecl];

            boolean found;
            log.info("Attempt to find key in KEY_GE mode: {}", key.getKeySidOnly(serviceId));
            found = zfile.locate(key.getKeyBytesSidOnly(serviceId),
                ZFileConstants.LOCATE_KEY_GE);

            log.info(RECORD_FOUND_MESSAGE, found);

            int overflowProtection = 10000;
            while (found) {
                int nread = zfile.read(recBuf);
                log.info("RecBuf: {}", recBuf);
                log.info("nread: {}", nread);

                log.info("ConvertedStringValue: {}", new String(recBuf, ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE));

                if (nread < 0) {
                    log.info("nread is < 0, stopping the retrieval");
                    found = false;
                    continue;
                }

                VsamRecord record = new VsamRecord(vsamConfig, serviceId, recBuf);
                log.info("Read record: {}", record);

                result.put(record.getKeyValue().getKey(), record.getKeyValue());

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
        } finally {
            closeZfile(zfile);
        }

        return result;
    }

    @SuppressWarnings({"squid:S1130", "squid:S1192"})
    public ZFile openZfile() throws ZFileException, RcException {
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

    public void closeZfile(ZFile zfile) {
        if (zfile != null) {
            try {
                zfile.close();
            } catch (ZFileException e) {
                log.error("Closing ZFile failed");
            }
        }
    }

}
