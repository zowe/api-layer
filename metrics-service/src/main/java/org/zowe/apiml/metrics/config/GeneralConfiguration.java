package org.zowe.apiml.metrics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.metrics.services.ZebraMetricsService;

@Configuration
public class GeneralConfiguration {

//    @Value("${zebra-metrics-url}")
    private String zebraBaseUrl = "https://zebra.talktothemainframe.com:3390/v1";

    @Bean
    public ZebraMetricsService zebraMetricsService(RestTemplate restTemplate) {
        return new ZebraMetricsService(zebraBaseUrl, restTemplate);
    }

}
