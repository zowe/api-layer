/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registration;

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Ties the hostname declared in a registration to the network identity of the registrant. The hostname must resolve
 * to the address the registration request was received from. Without this check any registrant could declare
 * the host and port of another service (e.g. ZAAS or Caching Service) and API ML would call that service on
 * the registrant's behalf (e.g. API Catalog retrieving the API documentation with its client certificate).
 * <p>
 * Replicated registrations are accepted only from the configured Discovery Service peers, since a replication
 * request carries registrations sent by other hosts.
 */
@Component
@Slf4j
public class RegistrantNetworkIdentityVerifier {

    @Value("${apiml.security.domains.networkIdentityValidationDisabled:false}")
    private boolean validationDisabled;

    @InjectApimlLogger
    private final ApimlLogger apimlLogger = ApimlLogger.empty();

    /**
     * @param info          registered instance
     * @param isReplication whether the registration is a replication from another Discovery Service
     * @param peerUrls      URLs of the other Discovery Services
     * @throws RegistrantNetworkIdentityException if the request does not come from the declared host
     */
    public void verify(InstanceInfo info, boolean isReplication, Collection<String> peerUrls) {
        var peerAddressOptional = RegistrationRequestContext.getPeerAddress();
        if (validationDisabled || peerAddressOptional.isEmpty()) {
            return;
        }
        var peerAddress = peerAddressOptional.get();

        if (isReplication) {
            var fromPeer = peerUrls.stream()
                .map(RegistrantNetworkIdentityVerifier::getHost)
                .filter(Objects::nonNull)
                .anyMatch(host -> isSameHost(peerAddress, host));
            if (!fromPeer) {
                apimlLogger.log("org.zowe.apiml.discovery.registration.replicationNotFromPeer", info.getInstanceId(), peerAddress.getHostAddress());
                throw new RegistrantNetworkIdentityException("Replication of instance " + info.getInstanceId() + " was not sent by a Discovery Service peer");
            }
        } else if (!isSameHost(peerAddress, info.getHostName())) {
            apimlLogger.log("org.zowe.apiml.discovery.registration.networkIdentityMismatch", info.getInstanceId(), info.getHostName(), peerAddress.getHostAddress());
            throw new RegistrantNetworkIdentityException("Hostname of instance " + info.getInstanceId() + " does not match the address of the registrant");
        }
    }

    private static String getHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            log.debug("Cannot parse the Discovery Service peer URL {}", url, e);
            return null;
        }
    }

    /**
     * @return true if the hostname resolves to the peer address, or both are addresses of this host (the request
     * was sent locally and the address chosen for the connection, e.g. loopback, could differ from the declared one)
     */
    boolean isSameHost(InetAddress peerAddress, String hostname) {
        if (StringUtils.isBlank(hostname)) {
            return false;
        }

        InetAddress[] hostAddresses;
        try {
            hostAddresses = InetAddress.getAllByName(hostname);
        } catch (UnknownHostException e) {
            log.debug("Hostname {} cannot be resolved", hostname, e);
            return false;
        }

        if (Arrays.asList(hostAddresses).contains(peerAddress)) {
            return true;
        }
        return isLocalAddress(peerAddress) && Arrays.stream(hostAddresses).anyMatch(this::isLocalAddress);
    }

    boolean isLocalAddress(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress()) {
            return true;
        }
        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (SocketException e) {
            log.debug("Cannot verify if {} is a local address", address, e);
            return false;
        }
    }

}
