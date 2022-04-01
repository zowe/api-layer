package org.zowe.apiml.metrics.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.metrics.RmfData;
import org.zowe.apiml.metrics.services.zebra.ZebraMetricsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ZebraMetricsServiceTest {
    private static final String baseUrl = "https://zebra.talktothemainframe.com:3390/v1";

    @Mock
    RestTemplate restTemplate;

    ZebraMetricsService fixture;

    @BeforeEach
    void setUp() {
        fixture = new ZebraMetricsService(baseUrl, restTemplate);
    }

    @Test
    public void TC01_validLparAndReport_getRmfData_returnMetricsWithTransformedTimestamps() {

        // MOCK INPUT
        String lpar = "RPRT";
        String report = "STOR";
        String expectedUrl = "https://zebra.talktothemainframe.com:3390/v1/RPRT/rmf3/STOR";
        RmfData mockMetricData = mockMetricData();
        when(restTemplate.getForObject(expectedUrl, RmfData.class)).thenReturn(mockMetricData());


        // EXECUTION
        RmfData result = fixture.getRmfData(lpar, report);

        // RESULT
        assertEquals(result.getTitle(), mockMetricData.getTitle());
        assertEquals(result.getColumnhead(), mockMetricData.getColumnhead());
        assertEquals(result.getTimeend(), "2022-03-31T20:10Z");
        assertEquals(result.getTimestart(), "2022-03-31T20:08Z");
        assertNull(result.getColumnhead());
        assertNull(result.getTable());
    }

    private RmfData mockMetricData() {
        return new RmfData("STOR (Storage Delays)", "03/31/2022 16:08:20", "03/31/2022 16:10:00", null, null);
    }

}
