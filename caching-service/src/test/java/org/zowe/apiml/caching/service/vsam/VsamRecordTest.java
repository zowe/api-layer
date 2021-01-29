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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.zfile.ZFileConstants;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
class VsamRecordTest {

    private VsamConfig config;

    private String longValue = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque ullamcorper ipsum fringilla tellus vestibulum, non sodales est faucibus. Praesent gravida, lacus sed eleifend iaculis, sem purus vulputate diam, congue consequat eros lectus quis erat. Integer et nisl justo. Duis sagittis, odio in cursus facilisis, neque mi facilisis odio, eu pulvinar est libero ac risus. In nulla ante, cursus sit amet imperdiet eget, consectetur et tellus. Vivamus bibendum facilisis mauris, sit amet fermentum tortor accumsan eu. Praesent congue mauris elit, non rhoncus orci lobortis vitae. Donec vulputate accumsan ante sit amet aliquam. Nunc faucibus leo id felis rhoncus, sit amet condimentum lectus euismod. Integer ultrices velit non posuere faucibus.\n" +
        "Integer laoreet sit amet nunc nec fringilla. Donec at nulla risus. Sed vitae tellus vel nibh rhoncus ornare at vel velit. Nullam ornare lacus purus, ut ullamcorper tellus pharetra luctus. Praesent eu facilisis lectus. Suspendisse pellentesque purus at ex mollis, eu porttitor.";

    @BeforeEach
    void prepareConfig() {
        this.config = mock(VsamConfig.class);
        when(config.getKeyLength()).thenReturn(30);
        when(config.getEncoding()).thenReturn(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        when(config.getRecordLength()).thenReturn(512);
    }

    @Test
    void bytesRepresentTheRecordStructure() throws UnsupportedEncodingException, VsamRecordException {
        String serviceId = "Service";
        KeyValue kv = new KeyValue("key", "value");
        VsamRecord underTest = new VsamRecord(config, serviceId, kv);

        assertThat(underTest.getBytes().length, is(512));
        assertThat(new String(underTest.getBytes(), ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE), containsString(String.valueOf(serviceId.hashCode())));
        assertThat(new String(underTest.getBytes(), ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE), containsString(String.valueOf(kv.getKey().hashCode())));
    }

    @Test
    void bytesHoldKeyValueSerializationInJsonFormat() throws UnsupportedEncodingException, VsamRecordException {
        String serviceId = "Service";
        KeyValue kv = new KeyValue("key", "value");
        VsamRecord underTest = new VsamRecord(config, serviceId, kv);

        String expectedSerialization = "{\"key\":\"key\",\"value\":\"value\",\"created\":\"" + kv.getCreated() + "\"}";
        assertThat(new String(underTest.getBytes(), ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE), containsString(expectedSerialization));
    }


    @Test
    void keyBytesRepresentTheKey() throws UnsupportedEncodingException, VsamRecordException {
        String serviceId = "Service";
        KeyValue kv = new KeyValue("key", "value");
        VsamRecord underTest = new VsamRecord(config, serviceId, kv);

        assertThat(underTest.getKeyBytes().length, is(30));
        assertThat(new String(underTest.getBytes(), ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE),
            startsWith(new String(underTest.getKeyBytes(), ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE)));
    }

    @Test
    void recordCanBeCreatedFromBytes() throws VsamRecordException, UnsupportedEncodingException {
        byte[] recordData = "-646160747:106079             {\"key\":\"daisy\",\"value\":\"flower\"}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             "
            .getBytes(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);

        VsamRecord underTest = new VsamRecord(config, recordData);

        assertThat(underTest.getKeyValue().getKey(), is("daisy"));
        assertThat(underTest.getKeyValue().getValue(), is("flower"));
    }

    @Test
    void recordCannotBeCreatedFromIllegalBytes() throws UnsupportedEncodingException {
        byte[] recordData = "-646160747:106079             {tank}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             "
            .getBytes(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);

        assertThrows(VsamRecordException.class, () -> new VsamRecord(config, recordData));
    }

    @Test
    void recordBytesThatExceedLengthOfRecordCannotBeCreated() {
        String serviceId = "Service";

        KeyValue kvLongValue = new KeyValue("key", longValue);
        VsamRecord underTest1 = new VsamRecord(config, serviceId, kvLongValue);

        assertThrows(StorageException.class, () -> underTest1.getBytes());

        KeyValue kvLongKey = new KeyValue(longValue, "value");
        VsamRecord underTest2 = new VsamRecord(config, serviceId, kvLongKey);

        assertThrows(StorageException.class, () -> underTest2.getBytes());

    }

    @Test
    void toStringCanBeCalledAfterRecordIsCreated() throws VsamRecordException, UnsupportedEncodingException {
        byte[] recordData = "-646160747:106079             {\"key\":\"daisy\",\"value\":\"flower\"}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             "
            .getBytes(ZFileConstants.DEFAULT_EBCDIC_CODE_PAGE);
        VsamRecord underTestFromBytes = new VsamRecord(config, recordData);
        assertDoesNotThrow(() -> underTestFromBytes.toString());

        VsamRecord underTestFromValues = new VsamRecord(config, "Service", new KeyValue("k","v"));
        assertDoesNotThrow(() -> underTestFromValues.toString());
    }

}
