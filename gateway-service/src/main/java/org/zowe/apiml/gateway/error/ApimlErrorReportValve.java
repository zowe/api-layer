package org.zowe.apiml.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.UDecoder;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import javax.servlet.ServletException;
import java.io.*;
import java.net.URLDecoder;


@Slf4j
public class ApimlErrorReportValve extends ValveBase {
    private final MessageService messageService;

    public ApimlErrorReportValve(MessageService messageService) {
        super(true);
        this.messageService = messageService;
    }

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        String msg;
        String Uri = request.getRequestURI();
        final boolean slashesAllowed = !UDecoder.ALLOW_ENCODED_SLASH;
        String decodedUri = URLDecoder.decode(Uri, "UTF-8");
        final boolean isRequestEncoded = !Uri.equals(decodedUri);

        Message message = messageService.createMessage("org.zowe.apiml.gateway.requestContainEncodedSlash", request.getRequestURI());
        if (slashesAllowed && isRequestEncoded) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpStatus.BAD_REQUEST.value());

            try {
                msg = new ObjectMapper().writeValueAsString(message.mapToView());
            } catch (JsonProcessingException e) {
                msg = message.mapToReadableText();
                log.debug("Could not convert response to JSON", e);
                throw e;
            }

            //TODO: write message to response
            //
            //

            response.setAppCommitted(true);
            apimlLog.log("org.zowe.apiml.gateway.requestContainEncodedSlash", request.getRequestURI());
        }
        else {
            getNext().invoke(request, response);
        }
    }
}
