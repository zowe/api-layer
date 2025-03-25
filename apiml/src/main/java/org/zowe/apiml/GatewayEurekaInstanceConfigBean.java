package org.zowe.apiml;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("apiml.gateway.eureka.instance")
public class GatewayEurekaInstanceConfigBean extends EurekaInstanceConfigBean {

    public GatewayEurekaInstanceConfigBean(InetUtils inetUtils) {
        super(inetUtils);
    }

}
