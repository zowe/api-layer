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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.zfile.ZFile;
import org.zowe.apiml.zfile.ZFileConstants;
import org.zowe.apiml.zfile.ZFileException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VsamFileTest {
    private VsamConfig vsamConfiguration;

    private VsamFile underTest;
    private VsamKey key;

    private ZFileProducer producer;
    private ZFile zFile;

    private final ApimlLogger apimlLogger = ApimlLogger.empty();

    private final String VALID_SERVICE_ID = "test-service-id";

    @BeforeEach
    void prepareConfig() throws VsamRecordException {
        vsamConfiguration = DefaultVsamConfiguration.defaultConfiguration();
        key = new VsamKey(vsamConfiguration);

        producer = mock(ZFileProducer.class);
        zFile = mock(ZFile.class);
        when(producer.openZfile()).thenReturn(zFile);
        underTest = new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.WRITE, false, producer, mock(VsamInitializer.class), apimlLogger);
    }

    @Nested
    class whenInstantiatingRecord {
        @Test
        void hasValidFileName() throws VsamRecordException {
            ZFileProducer producer = mock(ZFileProducer.class);

            when(producer.openZfile()).thenReturn(zFile);
            assertDoesNotThrow(() -> new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.READ, false, producer, mock(VsamInitializer.class), apimlLogger));
        }

        @Test
        void hasInvalidFileName() {
            vsamConfiguration.setFileName("test-file-name");
            assertThrows(IllegalArgumentException.class,
                () -> new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.READ, apimlLogger),
                "Expected exception is not IllegalArgumentException");
        }

        @Test
        void givenNullConfig_ExceptionIsThrown() {
            VsamInitializer initializer = new VsamInitializer();
            assertThrows(IllegalArgumentException.class, () -> new VsamFile(null, VsamConfig.VsamOptions.WRITE, false, null, initializer, apimlLogger));
        }

        @Test
        void givenOpenZFileThrowsException_ExceptionIsThrown() throws VsamRecordException {
            when(producer.openZfile()).thenThrow(VsamRecordException.class);
            VsamInitializer initializer = new VsamInitializer();
            assertThrows(IllegalStateException.class, () -> new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.WRITE, false, producer, initializer, apimlLogger));
        }
    }

    @Nested
    class whenReadingRecord {
        @Test
        void givenNullRecord_ExceptionIsThrown() {
            assertThrows(IllegalArgumentException.class, () -> {
                underTest.read(null);
            });
        }

        @Test
        void givenZFileThrowsError_TheErrorIsPropagated() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toRead = defaultVsamRecord();
            String createdKey = toRead.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, createdKey), ZFileConstants.LOCATE_KEY_EQ)).thenThrow(zFileException());
            assertThrows(RetryableVsamException.class, () -> underTest.read(toRead));
        }

        @Test
        void givenRecordDoesntExists_NoRecordIsReturned() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toRead = defaultVsamRecord();
            String createdKey = toRead.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, createdKey), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(false);
            assertFalse(underTest.read(toRead).isPresent());
        }

        @Test
        void givenExistingRecord_thenItIsReturned() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toRead = defaultVsamRecord();
            String readKey = toRead.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, readKey), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(true);
            when(zFile.read(any())).thenAnswer(prepareAnswer(1));

            Optional<VsamRecord> result = underTest.read(toRead);
            assertTrue(result.isPresent());
            assertThat(result.get().getKeyValue().getKey(), is(readKey));
        }
    }

    @Nested
    class whenUpdatingRecord {
        @Test
        void givenNullRecord_ExceptionIsThrown() {
            assertThrows(IllegalArgumentException.class, () -> {
                underTest.update(null);
            });
        }

        @Test
        void givenZFileThrowsError_TheErrorIsPropagated() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toUpdate = defaultVsamRecord();
            String createdKey = toUpdate.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, createdKey), ZFileConstants.LOCATE_KEY_EQ)).thenThrow(zFileException());
            assertThrows(RetryableVsamException.class, () -> underTest.update(toUpdate));
        }

        @Test
        void givenRecordDoesntExists_NoRecordIsReturned() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toUpdate = defaultVsamRecord();
            String createdKey = toUpdate.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, createdKey), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(false);
            assertFalse(underTest.update(toUpdate).isPresent());
        }

        @Test
        void givenExistingRecord_thenItIsUpdated() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toUpdate = defaultVsamRecord();
            String readKey = toUpdate.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, readKey), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(true);
            when(zFile.read(any())).thenAnswer(prepareAnswer(1));

            Optional<VsamRecord> result = underTest.update(toUpdate);
            assertTrue(result.isPresent());
            assertThat(result.get().getKeyValue().getKey(), is(readKey));
        }
    }

    @Nested
    class whenDeletingRecord {
        @Test
        void givenNullRecord_ExceptionIsThrown() {
            assertThrows(IllegalArgumentException.class, () -> {
                underTest.delete(null);
            });
        }

        @Test
        void givenZFileThrowsError_TheErrorIsPropagated() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toDelete = defaultVsamRecord();
            String deletedKey = toDelete.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, deletedKey), ZFileConstants.LOCATE_KEY_EQ)).thenThrow(zFileException());
            assertThrows(RetryableVsamException.class, () -> underTest.delete(toDelete));
        }

        @Test
        void givenRecordDoesntExists_NoRecordIsReturned() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toDelete = defaultVsamRecord();
            String deletedKey = toDelete.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, deletedKey), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(false);
            assertFalse(underTest.delete(toDelete).isPresent());
        }

        @Test
        void givenExistingRecord_ItIsDeleted() throws VsamRecordException, ZFileException {
            VsamRecord toDelete = defaultVsamRecord();

            when(zFile.locate(toDelete.getKeyBytes(), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(true);
            when(zFile.read(any())).thenAnswer(prepareAnswer(1));

            assertTrue(underTest.delete(toDelete).isPresent());

            verify(zFile).delrec();
        }
    }

    @Nested
    class whenCreatingRecord {
        @Test
        void givenNullRecord_ExceptionIsThrown() {
            assertThrows(IllegalArgumentException.class, () -> {
                underTest.create(null);
            });
        }

        @Test
        void givenRecordAlreadyExists_NoRecordIsReturned() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toCreate = defaultVsamRecord();
            String createdKey = toCreate.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, createdKey), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(true);
            assertFalse(underTest.create(toCreate).isPresent());
        }

        @Test
        void givenZFileThrowsError_TheErrorIsPropagated() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toCreate = defaultVsamRecord();
            String createdKey = toCreate.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, createdKey), ZFileConstants.LOCATE_KEY_EQ)).thenThrow(zFileException());
            assertThrows(RetryableVsamException.class, () -> underTest.create(toCreate));
        }

        @Test
        void givenValidRecord_thenItIsReturned() throws UnsupportedEncodingException, ZFileException {
            VsamRecord toCreate = defaultVsamRecord();
            String createdKey = toCreate.getKeyValue().getKey();

            when(zFile.locate(key.getKeyBytes(VALID_SERVICE_ID, createdKey), ZFileConstants.LOCATE_KEY_EQ)).thenReturn(false);
            assertTrue(underTest.create(toCreate).isPresent());
        }
    }

    @Nested
    class whenReadingForService {
        @Test
        void givenInvalidServiceId_ExceptionIsThrown() {
            assertThrows(IllegalArgumentException.class, () -> {
                underTest.readForService(null);
            });

            assertThrows(IllegalArgumentException.class, () -> {
                underTest.readForService("");
            });
        }

        @Test
        void givenValidServiceId_allServiceRecordsAreReturned() throws ZFileException, UnsupportedEncodingException {
            int amountOfReturnedRecords = 10;

            when(zFile.locate(key.getKeyBytesSidOnly(VALID_SERVICE_ID), ZFileConstants.LOCATE_KEY_GE)).thenReturn(true);
            when(zFile.read(any())).thenAnswer(prepareAnswer(amountOfReturnedRecords));

            List<VsamRecord> returnedRecords = underTest.readForService(VALID_SERVICE_ID);
            assertThat(returnedRecords, hasSize(amountOfReturnedRecords));
        }

        @Test
        void givenZfileThrowsError_thenEmptyListIsReturned() throws UnsupportedEncodingException, ZFileException {
            when(zFile.locate(key.getKeyBytesSidOnly(VALID_SERVICE_ID), ZFileConstants.LOCATE_KEY_GE))
                .thenThrow(zFileException());

            assertThat(underTest.readForService(VALID_SERVICE_ID), hasSize(0));
        }
    }

    @Nested
    class WhenDeletingForService {
        @Test
        void givenValidServiceId_allServiceRecordsAreReturned() throws ZFileException, UnsupportedEncodingException {
            int amountOfReturnedRecords = 1;

            when(zFile.locate(key.getKeyBytesSidOnly(VALID_SERVICE_ID), ZFileConstants.LOCATE_KEY_GE)).thenReturn(true);
            when(zFile.read(any())).thenAnswer(prepareAnswer(amountOfReturnedRecords));

            underTest.deleteForService(VALID_SERVICE_ID);
            verify(zFile).delrec();
        }
    }

    @Nested
    class whenClosingRecord {
        @Test
        void givenZFile_fileIsClosed() throws VsamRecordException, ZFileException {
            when(producer.openZfile()).thenReturn(zFile);
            VsamFile vsamFile = new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.READ, false, producer, mock(VsamInitializer.class), apimlLogger);
            vsamFile.close();

            verify(zFile).close();
        }

        @Test
        void givenNoZFile_fileIsNotClosed() throws VsamRecordException, ZFileException {
            when(producer.openZfile()).thenReturn(null);
            VsamFile vsamFile = new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.READ, false, producer, mock(VsamInitializer.class), apimlLogger);
            vsamFile.close();

            verify(zFile, times(0)).close();
        }

        @Test
        void givenZFileCloseError_NoExceptionThrown() throws VsamRecordException, ZFileException {
            doThrow(new ZFileException("", "", "", 0, 0, 0, new byte[]{}, 0, 0, 0, 0, 0)).when(zFile).close();
            when(producer.openZfile()).thenReturn(zFile);
            VsamFile vsamFile = new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.READ, false, producer, mock(VsamInitializer.class), apimlLogger);
            vsamFile.close();

            verify(zFile).close();
        }
    }

    @Nested
    class whenCountingRecords {
        @Test
        void givenExceptionReadingFile_thenReturnZero() throws ZFileException {
            when(zFile.read(any())).thenThrow(new ZFileException("", "", "", 0, 0, 0, new byte[]{}, 0, 0, 0, 0, 0));
            int result = underTest.countAllRecords();

            assertThat(result, is(0));
        }

        @Test
        void givenNoFileContent_thenReturnZero() throws ZFileException {
            when(zFile.read(any())).thenReturn(-1);
            int result = underTest.countAllRecords();

            assertThat(result, is(0));
        }

        @Test
        void givenRecords_thenReturnCount() throws ZFileException {
            when(zFile.read(any())).thenReturn(1).thenReturn(-1);
            int result = underTest.countAllRecords();

            assertThat(result, is(1));
        }
    }

    @Nested
    class whenInitialCreation {
        private VsamInitializer vsamInitializer;

        @BeforeEach
        void setInitialCreation() {
            vsamInitializer = mock(VsamInitializer.class);
        }

        @Test
        void givenVsamFileException_thenThrowException() throws ZFileException, VsamRecordException {
            doThrow(new RuntimeException("error")).when(vsamInitializer).warmUpVsamFile(any(), any());
            assertThrows(IllegalStateException.class, () -> underTest = new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.WRITE, true, producer, vsamInitializer, apimlLogger));
        }

        @Test
        void givenValidVsam_thenCreateVsamFile() {
            assertDoesNotThrow(() -> underTest = new VsamFile(vsamConfiguration, VsamConfig.VsamOptions.WRITE, true, producer, vsamInitializer, apimlLogger));
        }
    }

    @Test
    void givenProperAnswer_properBytesAreReturned() throws ZFileException {
        int validAmountOfBytes = 20;
        when(zFile.read(any())).thenReturn(validAmountOfBytes);

        Optional<byte[]> read = underTest.readBytes(new byte[]{});
        assertTrue(read.isPresent());
    }

    @Test
    void givenNothingIsRead_nothingIsReturned() throws ZFileException {
        int nothingFound = -1;
        when(zFile.read(any())).thenReturn(nothingFound);

        Optional<byte[]> read = underTest.readBytes(new byte[]{});
        assertFalse(read.isPresent());
    }


    private Answer<Integer> prepareAnswer(int amountOfReturnedRecords) {
        List<VsamRecord> records = recordsToReturn(amountOfReturnedRecords);

        return invocation -> {
            byte[] arrayToPopulate = invocation.getArgument(0);
            if (records.size() == 0) {
                return -1;
            }

            VsamRecord toReturn = records.remove(0);
            System.arraycopy(toReturn.getBytes(), 0, arrayToPopulate, 0, arrayToPopulate.length);
            return arrayToPopulate.length;
        };
    }

    private List<VsamRecord> recordsToReturn(int size) {
        List<VsamRecord> records = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            records.add(vsamRecord("key-" + i, "value-" + i, i));
        }

        return records;
    }

    private VsamRecord defaultVsamRecord() {
        return vsamRecord("key-0", "value-0", 0);
    }

    private VsamRecord vsamRecord(String key, String value, int created) {
        KeyValue keyValue = new KeyValue(key, value, String.valueOf(created));
        keyValue.setServiceId(VALID_SERVICE_ID);
        return new VsamRecord(vsamConfiguration, VALID_SERVICE_ID, keyValue);
    }

    private ZFileException zFileException() {
        return new ZFileException("", "", "", 0, 0, 0, null, 0, 0, 0, 0, 0);
    }
}
