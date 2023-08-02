package org.zowe.apiml.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final long PERIOD = Duration.ofSeconds(0).toMillis();

    private final Providers providers;
    private final ZosmfService zosmfService;

    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (providers.isZosfmUsed()) {
            log.debug(null);
            new Timer().scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    if (providers.isZosmfAvailableAndOnline()) {
                        cancel();
                        notifyStartup();
                    } else if () { // if it went over time?
                        cancel();
                    }
                }

            }, 0, PERIOD);
        } else {
            notifyStartup();
        }
    }

    private void notifyStartup() {
        new ServiceStartupEventHandler().onServiceStartup("Gateway Service",
            ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
