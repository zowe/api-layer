const eureka = require("eureka-js-client");

function apiLayerServiceModule() {
    this.registerService = function(options) {
        const metadata = {
            "apiml.apiInfo.0.apiId": options.apiInfo[0].apiId,
            "apiml.apiInfo.0.gatewayUrl": options.apiInfo[0].gatewayUrl,
            "apiml.apiInfo.0.swaggerUrl": options.apiInfo[0].swaggerUrl,
            "apiml.discovery.catalogUiTile.description": options.catalogUiTile.description,
            "apiml.discovery.catalogUiTile.id": options.catalogUiTile.tileId,
            "apiml.discovery.catalogUiTile.title": options.catalogUiTile.title,
            "apiml.discovery.catalogUiTile.version": options.catalogUiTile.version,
            "apiml.discovery.service.title": options.title,
            "apiml.discovery.service.description": options.description,
            "routed-services.0.gateway-url": options.routes[0].gatewayUrl,
            "routed-services.0.service-url": options.routes[0].serviceRelativeUrl
        };

        const client = new eureka.Eureka({
            instance: {
                app: options.serviceId,
                instanceId: `${options.hostName}:${options.serviceId}:${options.port}`,
                hostName: options.hostName,
                ipAddr: options.ipAddr,
                homePageUrl: options.homePageUrl,
                statusPageUrl: options.statusPageUrl,
                healthCheckUrl: options.healthCheckUrl,
                secureHealthCheckUrl: options.healthCheckUrl,
                port: {
                    $: options.port,
                    "@enabled": "false"
                },
                securePort: {
                    $: options.port,
                    "@enabled": "true"
                },
                vipAddress: options.serviceId,
                secureVipAddress: options.serviceId,
                dataCenterInfo: {
                    "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                    name: "MyOwn"
                },
                metadata: metadata
            },
            eureka: {
                ssl: true,
                serviceUrls: {
                    default: [options.discoveryServiceUrl]
                }
            },
            requestMiddleware: (requestOpts, done) => {
                requestOpts.pfx = options.tlsOptions.pfx;
                requestOpts.ca = options.tlsOptions.ca;
                requestOpts.cert = options.tlsOptions.cert;
                requestOpts.key = options.tlsOptions.key;
                requestOpts.passphrase = options.tlsOptions.passphrase;
                done(requestOpts);
            }
        });

        client.start();
        return client;
    };
}

module.exports = apiLayerServiceModule;
