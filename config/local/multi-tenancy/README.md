# Deploying Services Locally Using Multi-Tenancy Setup

Start the services using the configuration in the [central](./central) and [domain](./domain) folders.

## Setting Up Mutual Registration
For mutual registration of the Central Gateway and Domain Gateway, you need to set the following environment variables to specify the discovery service URLs for registration:

1. **Domain Gateway**:
    Set the following environment variable: `ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS=https://localhost:10011/eureka`

2. **Central Gateway**:
    Set the following environment variable: `ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS=https://localhost:10021/eureka`
