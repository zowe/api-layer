package org.zowe.apiml.gateway.error;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.UDecoder;
import org.springframework.http.HttpStatus;

import java.io.*;
import java.net.URLDecoder;

public class ApimlErrorReportValve extends ValveBase {

    public ApimlErrorReportValve() {
        super(true);
    }


    @Override
    public void invoke(Request request, Response response) throws IOException {


        final boolean slashesAllowed = !UDecoder.ALLOW_ENCODED_SLASH;
        String Uri = request.getRequestURI();
        String decodedUri = URLDecoder.decode(Uri,"UTF-8");
        final boolean isRequestEncoded = !Uri.equals(decodedUri);

        if (slashesAllowed && isRequestEncoded) {

            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED.value());
            out.print("blah blah blah");
            out.flush();

            response.setAppCommitted(true);
            response.finishResponse();
        }
    }
}
