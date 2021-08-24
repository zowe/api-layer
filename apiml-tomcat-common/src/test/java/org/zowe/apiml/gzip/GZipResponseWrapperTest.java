/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gzip;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class GZipResponseWrapperTest {

    @Test
    void givenStringOnInput_thenCompress() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<breakfast_menu>\n" +
            "  <food>\n" +
            "    <name>Belgian Waffles</name>\n" +
            "    <price>$5.95</price>\n" +
            "    <description>Two of our famous Belgian Waffles with plenty of real maple syrup</description>\n" +
            "    <calories>650</calories>\n" +
            "  </food>\n" +
            "  <food>\n" +
            "    <name>Strawberry Belgian Waffles</name>\n" +
            "    <price>$7.95</price>\n" +
            "    <description>Light Belgian waffles covered with strawberries and whipped cream</description>\n" +
            "    <calories>900</calories>\n" +
            "  </food>\n" +
            "  <food>\n" +
            "    <name>Berry-Berry Belgian Waffles</name>\n" +
            "    <price>$8.95</price>\n" +
            "    <description>Light Belgian waffles covered with an assortment of fresh berries and whipped cream</description>\n" +
            "    <calories>900</calories>\n" +
            "  </food>\n" +
            "  <food>\n" +
            "    <name>French Toast</name>\n" +
            "    <price>$4.50</price>\n" +
            "    <description>Thick slices made from our homemade sourdough bread</description>\n" +
            "    <calories>600</calories>\n" +
            "  </food>\n" +
            "  <food>\n" +
            "    <name>Homestyle Breakfast</name>\n" +
            "    <price>$6.95</price>\n" +
            "    <description>Two eggs, bacon or sausage, toast, and our ever-popular hash browns</description>\n" +
            "    <calories>950</calories>\n" +
            "  </food>\n" +
            "</breakfast_menu>";
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gZipOutputStream = new GZIPOutputStream(arrayOutputStream);

        MockHttpServletResponse response = new MockHttpServletResponse();
        GZipResponseWrapper responseWrapper = new GZipResponseWrapper(response, gZipOutputStream);
        byte [] originalBytes = xml.getBytes();
        responseWrapper.getOutputStream().write(originalBytes);
        responseWrapper.getOutputStream().close();
        byte [] compressed  = arrayOutputStream.toByteArray();
        int compressLength = compressed.length;
        assertTrue(compressLength < originalBytes.length);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
        GZIPInputStream inputStream = new GZIPInputStream(byteArrayInputStream);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        String output = "";
        while((line = bufferedReader.readLine()) != null){
            output += line + "\n";
        }
        output = output.substring(0, output.length() - 1);
        inputStream.close();
        assertEquals(xml,output);
    }
}
