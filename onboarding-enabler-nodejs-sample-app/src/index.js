/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

const express = require("express");
const https = require("https");
const apiLayerService = require("@zowe/apiml-onboarding-enabler-nodejs");

// Command-line arguments:
const args = {
    port: 10020,
    serviceId: "hwexpress",
    // On z/OS, you need to use certificates encoded in EBCDIC
    // The APIML stores such certificates in files with `-ebcdic` suffix
};

/**
 * Registers the service to the APIML Discovery service
 */
/**
 * Starts the REST API service as an HTTPS server
 */
function startHttpsService() {
    const app = express();

    // Index page with a link to the REST API endpoint:
    app.get("/", (req, res) =>
        res.json({
            links: [
                {
                    rel: "hello",
                    href: `${req.protocol}://${req.get("Host")}/api/v1/hello`
                }
            ]
        })
    );

    // REST API endpoint:
    app.get("/api/v1/hello", (req, res) => res.json({ greeting: "Hello World!" }));

    // Status and health endpoints for Eureka:
    app.get("/api/v1/info", (req, res) => res.json({ serviceId: args.serviceId, nodeJsVersion: process.version }));
    app.get("/api/v1/status", (req, res) => res.json({ status: "UP" }));

    // Static resources (contains Swagger JSON document with API documentation):
    app.use(express.static("static"));

    // Start HTTPS server and register to Discovery Service:
    tlsOptions = apiLayerService.tlsOptions;
    const httpsServer = https.createServer(tlsOptions, app);
    httpsServer.listen(args.port, function () {
        console.log(`${args.serviceId} service listening on port ${args.port}`);
        apiLayerService.connectToEureka();
    });
}

function exitHandler() {
    apiLayerService.unregisterFromEureka();
}

process.on('SIGTERM', signal => {
    exitHandler();
    process.exit(0);
});

process.on('SIGINT', signal => {
    exitHandler();
    process.exit(0);
});

process.on('uncaughtException', err => {
    exitHandler();
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    exitHandler();
    process.exit(1);
});

startHttpsService();
