package org.zowe.apiml.gateway.filters.pre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class ExtractAuthSourceFilter extends OncePerRequestFilter {
    private static final String AUTH_SOURCE_ATTR = "zaas.auth.source";

    private final AuthSourceService authSourceService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        extractAuthSource(request);
        filterChain.doFilter(request, response);
    }

    private void extractAuthSource(HttpServletRequest request) {
        Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
        if (authSource.isPresent()) {
            AuthSource.Parsed parsed = authSourceService.parse(authSource.get());
            request.setAttribute(AUTH_SOURCE_ATTR, parsed);
        }
    }
}
