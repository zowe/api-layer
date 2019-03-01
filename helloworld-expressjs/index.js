const express = require("express");
const https = require("https");
const fs = require("fs");
const apiLayerService = require("./apiLayerService");

// Command-line arguments:
const args = {
    port: process.argv[2] || 10020,
    hostName: process.argv[3] || "localhost",
    ipAddr: process.argv[4] || "127.0.0.1",
    serviceId: process.argv[5] || "hwexpress",
    discoveryServiceUrl: process.argv[6] || "https://localhost:10011/eureka/apps/",
    // On z/OS, you need to use certificates encoded in EBCDIC
    // The APIML stores such certificates in files with `-ebcdic` suffix
    cert: process.argv[7] || "../keystore/localhost/localhost.keystore.cer",
    key: process.argv[8] || "../keystore/localhost/localhost.keystore.key",
    ca: process.argv[9] || "../keystore/local_ca/localca.cer",
    // pfx: process.argv[7] || "../keystore/localhost/localhost.keystore.p12",
    // passphrase: process.argv[8] || "password"
};

// Options for TLS (shared by the HTTPS server and Eureka client):
const tlsOptions = {
    cert: fs.readFileSync(args.cert),
    key: fs.readFileSync(args.key),
    ca: fs.readFileSync(args.ca),
    // pfx: fs.readFileSync(args.pfx),
    // passphrase: args.passphrase,
    secureProtocol: "TLSv1_2_method",
    rejectUnauthorized: true
};

/**
 * Registers the service to the APIML Discovery service
 */
function registerServiceToDiscoveryService() {
    new apiLayerService().registerService({
        // See README.md for more details about following options
        tlsOptions: tlsOptions,
        discoveryServiceUrl: args.discoveryServiceUrl,
        serviceId: args.serviceId,
        title: "Hello World API Service in Express",
        description: "Hello World REST API Service implemented in Express and Node.js",
        hostName: args.hostName,
        ipAddr: args.ipAddr,
        port: args.port,
        homePageUrl: `https://${args.hostName}:${args.port}/`,
        statusPageUrl: `https://${args.hostName}:${args.port}/info`,
        healthCheckUrl: `https://${args.hostName}:${args.port}/status`,
        routes: [
            {
                gatewayUrl: "api/v1",
                serviceRelativeUrl: "/api/v1"
            }
        ],
        apiInfo: [
            {
                apiId: "org.zowe.hwexpress",
                gatewayUrl: "api/v1",
                swaggerUrl: `https://${args.hostName}:${args.port}/swagger.json`
            }
        ],
        catalogUiTile: {
            tileId: "cademoapps",
            title: "Sample API Mediation Layer Applications",
            description:
                "Applications which demonstrate how to make a service integrated to the API Mediation Layer ecosystem",
            version: "1.0.0"
        }
    });
}

/**
 * Starts the REST API service as an HTTPS server
 */
function startHttpsService() {
    const app = express();

    // Index page with a link to the REST API endpoint:
    app.get("/", (req, res) = >
    res.json({
        links: [
            {
                rel: "hello",
                href: `${req.protocol}://${req.get("Host")}/api/v1/hello`
            }
        ]
    })
)
    ;

    // REST API endopint:
    app.get("/api/v1/hello", (req, res) = > res.json({greeting: "Hello World!"})
)
    ;

    // Status and health endpoints for Eureka:
    app.get("/info", (req, res) = > res.json({serviceId: serviceId, nodeJsVersion: process.version})
)
    ;
    app.get("/status", (req, res) = > res.json({status: "UP"})
)
    ;

    // Static resoures (contains Swagger JSON document with API documentation):
    app.use(express.static("static"));

    // Start HTTPS server:
    const httpsServer = https.createServer(tlsOptions, app);
    httpsServer.listen(args.port, function () {
        console.log(`${args.serviceId} service listening on port ${args.port}`);
        registerServiceToDiscoveryService();
    });
}

startHttpsService();
