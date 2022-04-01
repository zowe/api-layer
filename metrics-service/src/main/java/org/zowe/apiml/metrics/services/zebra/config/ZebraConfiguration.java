package org.zowe.apiml.metrics.services.zebra.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(value = "metrics.system.zebra")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "metrics.system.service", havingValue = "zebra")
public class ZebraConfiguration {
    private String baseUrl;
}
