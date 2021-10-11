package org.zowe.apiml.acceptance.config;

import com.netflix.zuul.ZuulFilter;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.gateway.security.service.PassTicketException;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

public class PassTicketFailureConfig {

    @Bean
    public ZuulFilter zuulFilter() {
        return new ZuulFilter() {
            @Override
            public String filterType() {
                return PRE_TYPE;
            }

            @Override
            public int filterOrder() {
                return 0;
            }

            @Override
            public boolean shouldFilter() {
                return true;
            }

            @Override
            public Object run() {
                throw new PassTicketException("problem");
            }
        };
    }
}
