package org.zowe.apiml.gateway.filters.pre;


import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.CounterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

class ServiceNotFoundFilterTest {
    private ZuulFilter underTest;
    private RequestContextProvider provider;

    @BeforeEach
    public void prepareFilterUnderTest() {
        provider = Mockito.mock(RequestContextProvider.class);
        underTest = new ServiceNotFoundFilter(provider);
    }

    @Test
    public void givenThereIsNoServiceId_whenTheUserRequestsThePath_then404IsProperlyReturned() {
        initializeCounterFactory();
        when(provider.context()).thenReturn(new RequestContext());

        Boolean ignoreThisFilter = underTest.shouldFilter();
        assertThat(ignoreThisFilter, is(true));

        // Exception represents 404
        assertThrows(ZuulException.class, () -> {
            underTest.run();
        });
    }

    @Test
    public void givenThereIsValidServiceId_whenTheUserRequestsThePath_thenThisFilterIsIgnored() {
        RequestContext context = new RequestContext();
        context.set(SERVICE_ID_KEY, "validServiceId");
        when(provider.context()).thenReturn(context);

        Boolean ignoreThisFilter = underTest.shouldFilter();
        assertThat(ignoreThisFilter, is(false));
    }

    /**
     * Zuul Exception requires this to be properly set.
     */
    public void initializeCounterFactory() {
        CounterFactory.initialize(new CounterFactory() {
            @Override
            public void increment(String name) {
            }
        });
    }
}
