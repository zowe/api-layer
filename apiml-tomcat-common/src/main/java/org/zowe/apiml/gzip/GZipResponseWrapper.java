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


import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

public class GZipResponseWrapper extends HttpServletResponseWrapper {

    private GZipServletOutputStream gzipOutputStream;
    private PrintWriter printWriter = null;
    private boolean disableFlushBuffer = false;

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response The response to be wrapped
     * @throws IllegalArgumentException if the response is null
     */
    public GZipResponseWrapper(HttpServletResponse response, GZIPOutputStream stream) {
        super(response);
        gzipOutputStream = new GZipServletOutputStream(stream);
    }

    public void close() throws IOException {
        if (this.printWriter != null) {
            this.printWriter.close();
        }

        if (this.gzipOutputStream != null) {
            this.gzipOutputStream.close();
        }
    }

    /**
     * Flush OutputStream or PrintWriter
     *
     * @throws IOException
     */
    @Override
    public void flushBuffer() throws IOException {
        flush();

        // doing this might leads to response already committed exception
        // when the PageInfo has not yet built but the buffer already flushed
        // Happens in Weblogic when a servlet forward to a JSP page and the forward
        // method trigger a flush before it forwarded to the JSP
        // disableFlushBuffer for that purpose is 'true' by default
        if (!disableFlushBuffer) {
            super.flushBuffer();
        }
    }

    /**
     * Flushes all the streams for this response.
     */
    public void flush() throws IOException {
        if (printWriter != null) {
            printWriter.flush();
        }

        if (gzipOutputStream != null) {
            gzipOutputStream.flush();
        }
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (this.printWriter != null) {
            throw new IllegalStateException(
                "PrintWriter obtained already - cannot get OutputStream");
        }

        return this.gzipOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.printWriter == null) {
            this.gzipOutputStream = new GZipServletOutputStream(
                getResponse().getOutputStream());

            this.printWriter = new PrintWriter(new OutputStreamWriter(
                this.gzipOutputStream, getResponse().getCharacterEncoding()), true);
        }

        return this.printWriter;
    }


    @Override
    public void setContentLength(int length) {
        //ignore, since content length of zipped content
        //does not match content length of unzipped content.
    }


    /**
     * Set if the wrapped reponse's buffer flushing should be disabled.
     *
     * @param disableFlushBuffer true if the wrapped reponse's buffer flushing should be disabled
     */
    public void setDisableFlushBuffer(boolean disableFlushBuffer) {
        this.disableFlushBuffer = disableFlushBuffer;
    }

}
