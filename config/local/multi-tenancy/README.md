# Deploying Services Locally Using Multi-Tenancy Setup

Start the Domain services using the additional configuration in the [domain](./domain) folder, which overrides certain properties, along with the configuration in the [local](../../local).
You can do that by passing multiple files via `spring.config.additional-location`.

**Example:**

`spring.config.additional-location=file:./config/local/api-catalog-service.yml,file:./config/local/multi-tenancy/domain/api-catalog-service.yml`

## Setting Up Mutual Registration
For mutual registration of the Central Gateway and Domain Gateway, you need to set the following environment variables to specify the discovery service URLs for registration:

1. **Domain Gateway**:
    Set the following environment variable: `ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS=https://localhost:10011/eureka`

2. **Central Gateway**:
    Set the following environment variable: `ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS=https://localhost:10021/eureka`
