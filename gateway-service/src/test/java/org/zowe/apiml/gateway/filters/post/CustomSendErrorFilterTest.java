package org.zowe.apiml.gateway.filters.post;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@SpringBootTest(properties="property.value=dummyValue")
@SpringBootConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
class CustomSendErrorFilterTest {
    private static final String SERVICE_ID = "serviceId";

    private CustomSendErrorFilter filter = null;
    private RequestContext ctx;

    @BeforeEach
    void setUp() {
        ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(SERVICE_ID_KEY, SERVICE_ID);
        ctx.setThrowable(new Exception("test"));
        ctx.set(CustomSendErrorFilter.ERROR_FILTER_RAN, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ctx.setRequest(request);
        ctx.setResponse(response);

        this.filter = new CustomSendErrorFilter();
    }

    @Test
    void givenContextToRun_whenTestIfFilterRuns_shouldRunErrorFilter() {
        assertTrue(filter.shouldFilter());
    }

    @Test
    void givenContextToNotRun_whenTestIfFilterRuns_shouldRunErrorFilter() {
        ctx.set(CustomSendErrorFilter.ERROR_FILTER_RAN, true);
        assertFalse(filter.shouldFilter());
    }

    @Test
    void givenFilter_whenGetOrder_ShouldBeOne() {
        int filterOrder = filter.filterOrder();
        assertEquals(1, filterOrder);
    }
}
