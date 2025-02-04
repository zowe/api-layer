package org.zowe.apiml.discovery.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ComponentScan({
    "org.zowe.apiml.security.common",
    "org.zowe.apiml.gateway.security.login"
})
@Profile("!https & !attls")
public class HttpWebSecurityLoginConfig {
}
