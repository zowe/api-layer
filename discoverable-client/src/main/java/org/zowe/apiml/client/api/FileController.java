/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * Version 1 of the controller that returns a zip file.
 */
@RestController
@Tag(name = "Other Operations")
public class FileController {
    private final ServletContext servletContext;

    public FileController(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @GetMapping(value = "/api/v1/get-file", produces = "image/png")
    @HystrixCommand()
    public ResponseEntity<InputStreamResource> downloadImage() {
        String fileName = "api-catalog.png";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
        InputStreamResource resource = new InputStreamResource(inputStream);
        String mineType = servletContext.getMimeType(fileName);
        MediaType mediaType = MediaType.parseMediaType(mineType);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
            .body(resource);
    }

    @GetMapping(value = "/api/v1/throttling-data", produces = "text/plain")
    public ResponseEntity<InputStreamResource> throttlingData(
        @RequestParam(value = "length", defaultValue = "1048576") long length,
        @RequestParam(value = "speed", defaultValue = "1024") long bytesPerSecond,
        @RequestParam(value = "provideLength", defaultValue = "false") boolean provideLength
    ) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
            .contentType(TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment;filename=%d_bytes_%d_bytes_per_second.txt", length, bytesPerSecond));
        if (provideLength) {
            response.contentLength(length);
        }
        return response.body(new InputStreamResource(new DummyDataThrottlingOutputStream(length, bytesPerSecond)));
    }

    @GetMapping(value = "/api/v1/data", produces = "text/plain")
    public ResponseEntity<InputStreamResource> throttlingData(
        @RequestParam(value = "length", defaultValue = "1048576") long length,
        @RequestParam(value = "provideLength", defaultValue = "false") boolean provideLength
    ) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
            .contentType(TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment;filename=%d_bytes.txt", length));
        if (provideLength) {
            response.contentLength(length);
        }
        return response.body(new InputStreamResource(new DummyDataInputStream(length)));
    }

    static class DummyDataInputStream extends InputStream {

        private static final String[] UNIT = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};

        private final long length;
        protected long produced = 0;
        private final byte[] buffer = new byte[1024];
        private final ByteArrayInputStream bufferInputStream = new ByteArrayInputStream(buffer);

        DummyDataInputStream(long length) {
            this.length = length;
            updateBuffer();
        }

        private String getMarkup(long value, int level) {
            StringBuilder out = new StringBuilder();
            if (value >= 1024) {
                out.append(getMarkup(value / 1024, level + 1));
                value = value % 1024;
            }
            if (value > 0) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(value).append(UNIT[level]);
            }
            return out.toString();
        }

        private void updateBuffer() {
            Arrays.fill(buffer, (byte) '.');
            long next = produced + 1024 - (produced % 1024);
            byte[] suffix = getMarkup(next, 0).getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < suffix.length; i++) {
                buffer[1024 + i - suffix.length] = suffix[i];
            }

            bufferInputStream.reset();
        }

        @Override
        public int read() {
            if (produced >= length) return -1;

            if (bufferInputStream.available() <= 0) {
                updateBuffer();
            }

            produced++;
            return bufferInputStream.read();
        }

    }

    static class DummyDataThrottlingOutputStream extends DummyDataInputStream {

        final long start = System.currentTimeMillis();
        final long bytesPerSecond;

        DummyDataThrottlingOutputStream(long length, long bytesPerSecond) {
            super(length);
            this.bytesPerSecond = bytesPerSecond;
        }

        @Override
        public int read() {
            long duration = System.currentTimeMillis() - start;

            long expectedMs = 1000 * (produced + 1) / bytesPerSecond;
            long toWait = expectedMs - duration;
            if (toWait > 0) {
                try {
                    Thread.sleep(toWait);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }

            return super.read();
        }

    }

}
