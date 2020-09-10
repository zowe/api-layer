package org.zowe.apiml.security.common.error;

import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DefaultExceptionHandler extends DefaultHandlerExceptionResolver {

    public DefaultExceptionHandler() {
        setOrder(Ordered.LOWEST_PRECEDENCE - 1); // Run before DefaultHandlerExceptionResolver
    }

    /**
     * Overrides {@link org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver} and
     * {@link DefaultHandlerExceptionResolver}to provide custom default exception resolution.
     * The same default functionality is used, however, logging is removed to avoid unhelpful logs being printed.
     */
    @Override
    @Nullable
    public ModelAndView resolveException(
        @NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @Nullable Object handler, @NonNull Exception ex) {

        if (shouldApplyTo(request, handler)) {
            prepareResponse(ex, response);
            return doResolveException(request, response, handler, ex);
        } else {
            return null;
        }
    }
}
