# Application configuration

IJO="$IJO -Denvironment.hostname=${{PFX}}_{{SYSN}}_HOSTNAME"
IJO="$IJO -Denvironment.port=${{PFX}}_{{SRV}}_PORT"
IJO="$IJO -Denvironment.discoveryLocations=${{PFX}}_EUREKA"
IJO="$IJO -Denvironment.ipAddress=${{PFX}}_{{SYSN}}_IP_ADDRESS"
IJO="$IJO -Denvironment.preferIpAddress=${{PFX}}_PREFER_IP_ADDRESS"
IJO="$IJO -Denvironment.gatewayHostname=${{PFX}}_GATEWAY_HOSTNAME"
IJO="$IJO -Denvironment.eurekaUserId=${{PFX}}_DISCOVERY_SERVICE_USERID"
IJO="$IJO -Denvironment.eurekaPassword=${{PFX}}_DISCOVERY_SERVICE_PASSWORD"

# SSL configuration:
IJO="$IJO -Denvironment.truststore=${{PFX}}_GW_SSL_TRUST_STORE"
IJO="$IJO -Denvironment.truststoreType=${{PFX}}_GW_SSL_TRUST_STORE_TYPE"
_VALUE="${{PFX}}_GW_SSL_TRUST_STORE_PASSWORD"
IJO="$IJO -Denvironment.truststorePassword=$_VALUE"

