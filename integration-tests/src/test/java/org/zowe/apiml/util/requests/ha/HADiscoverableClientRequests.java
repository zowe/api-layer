package org.zowe.apiml.util.requests.ha;

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.util.requests.DiscoverableClientRequests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zowe.apiml.util.config.ConfigReader.environmentConfiguration;

@Slf4j
public class HADiscoverableClientRequests {

    public List<DiscoverableClientRequests> discoverableClients = new ArrayList<>();

    public HADiscoverableClientRequests() {
        String[] discoverableClientHosts = environmentConfiguration().getDiscoverableClientConfiguration().getHost().split(",");
        for (String host: discoverableClientHosts) {
            discoverableClients.add(new DiscoverableClientRequests(host));
        }
        log.info("Created HADiscoverableClientRequests");
    }

    /**
     * Return the number of instances.
     */
    public int existing() {
        return discoverableClients.size();
    }

    /**
     * Check whether a specific instance is UP.
     * @param instance the specific instance
     * @return true if UP
     */
    public boolean up(int instance) {
        return discoverableClients.get(instance).isUp();
    }

    /**
     * Check whether all the instances are UP.
     * @return true if UP
     */
    public boolean up() {
        AtomicBoolean allUp = new AtomicBoolean(true);

        discoverableClients.parallelStream().forEach(service -> {
            if (!service.isUp()) {
                allUp.set(false);
            }
        });
        return allUp.get();
    }

    /**
     * Shutdown a specific instance of the service.
     * @param instance the specific instance
     */
    public void shutdown(int instance) {
        discoverableClients.get(instance).shutdown();
    }

    /**
     * Shutdown all the instances of the service.
     */
    public void shutdown() {
        discoverableClients.parallelStream()
            .forEach(DiscoverableClientRequests::shutdown);
    }

}
