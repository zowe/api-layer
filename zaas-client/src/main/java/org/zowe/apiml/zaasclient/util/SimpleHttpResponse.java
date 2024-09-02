package org.zowe.apiml.zaasclient.util;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.util.Map;

/**
 * Simple record to hold data extracted from {@link org.apache.hc.core5.http.ClassicHttpResponse} so the underlying
 * resources can be closed and the data processed further.
 * Intended to be used with {@link org.apache.http.impl.client.CloseableHttpClient} execute methods as return
 * value of {@link org.apache.http.client.ResponseHandler} in case of custom exceptions needs to be thrown during response processing.
 *
 * @param code       http status code
 * @param byteBody   response body as byte[]
 * @param stringBody response body as String
 * @param headers    http response headers
 */

public record SimpleHttpResponse(
    int code,
    byte[] byteBody,
    String stringBody,
    Map<String, Header> headers
) {

    public SimpleHttpResponse(int code, byte[] byteBody) {
        this(code, byteBody, null, null);
    }

    public SimpleHttpResponse(int code, String stringBody) {
        this(code, null, stringBody, null);
    }

    public SimpleHttpResponse(int code, String stringBody, Map<String, Header> headers) {
        this(code, null, stringBody, headers);
    }

    public SimpleHttpResponse(int code) {
        this(code, null, null, null);
    }

    public SimpleHttpResponse(int code, Map<String, Header> headers) {
        this(code, null, null, headers);
    }

    /**
     * Extract data from http response so the underlying resources of {@link org.apache.hc.core5.http.ClassicHttpResponse} can be closed.
     * On success http status (2XX), data are fetched as byte[] into byteBody, otherwise as String into stringBody.
     *
     * @param response http response to fetch data from
     * @return {@link SimpleHttpResponse}
     * @throws IOException    in case of data cannot be fetched from the response stream
     * @throws ParseException in case of data cannot be converted into String
     */
    public static SimpleHttpResponse fromResponseWithBytesBodyOnSuccess(ClassicHttpResponse response) throws IOException, ParseException {
        if (response.getEntity() != null) {
            if (isSuccessInternal(response.getCode())) {
                return new SimpleHttpResponse(response.getCode(), response.getEntity().getContent().readAllBytes());
            } else {
                return new SimpleHttpResponse(response.getCode(), EntityUtils.toString(response.getEntity()));
            }
        } else {
            return new SimpleHttpResponse(response.getCode());
        }
    }

    public boolean isSuccess() {
        return isSuccessInternal(code);
    }

    private static boolean isSuccessInternal(int code) {
        return code >= HttpStatus.SC_OK && code < HttpStatus.SC_MULTIPLE_CHOICES;
    }
}
