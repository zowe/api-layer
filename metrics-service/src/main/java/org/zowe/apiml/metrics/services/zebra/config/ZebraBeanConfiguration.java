package org.zowe.apiml.metrics.services.zebra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.metrics.services.zebra.ZebraMetricsService;

@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(name = "metrics.system.service", havingValue = "zebra")
public class ZebraBeanConfiguration {

    private final ZebraConfiguration zebraConfiguration;

    @Bean
    public ZebraMetricsService zebraMetricsService(RestTemplate restTemplate) {
        return new ZebraMetricsService(zebraConfiguration.getBaseUrl(), restTemplate);
    }

}
