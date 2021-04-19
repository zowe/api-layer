# Application configuration

IJO="$IJO -Dapiml.service.hostname=${{PFX}}_{{SYSN}}_HOSTNAME"
IJO="$IJO -Dapiml.service.port=${{PFX}}_{{SRV}}_PORT"
IJO="$IJO -Dapiml.service.discoveryServiceUrls=${{PFX}}_EUREKA"
IJO="$IJO -Dapiml.service.ipAddress=${{PFX}}_{{SYSN}}_IP_ADDRESS"
IJO="$IJO -Denvironment.preferIpAddress=${{PFX}}_PREFER_IP_ADDRESS"
IJO="$IJO -Denvironment.gatewayHostname=${{PFX}}_GATEWAY_HOSTNAME"
IJO="$IJO -Denvironment.eurekaUserId=${{PFX}}_DISCOVERY_SERVICE_USERID"
IJO="$IJO -Denvironment.eurekaPassword=${{PFX}}_DISCOVERY_SERVICE_PASSWORD"

# SSL configuration:
IJO="$IJO -Denvironment.truststore=${{PFX}}_GW_SSL_TRUST_STORE"
IJO="$IJO -Denvironment.truststoreType=${{PFX}}_GW_SSL_TRUST_STORE_TYPE"
_VALUE="${{PFX}}_GW_SSL_TRUST_STORE_PASSWORD"
IJO="$IJO -Denvironment.truststorePassword=$_VALUE"

