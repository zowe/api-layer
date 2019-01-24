const apimlAuth = require("./apimlAuth");
const fs = require('fs');
const assert = require('assert');

describe("apimlAuth", function () {
    const pluginDef = JSON.parse(fs.readFileSync('pluginDefinition.json'));
    const pluginConf = {};
    const serverConf = {
        node: {
            "https": {
                "certificateAuthorities": ["../keystore/local_ca/localca.cer"]
            },
            "http": {
                "port": 8543
            },
            "mediationLayer": {
                "server": {
                    "hostname": "localhost",
                    "port": 10011,
                    "isHttps": false,
                    "gatewayPort": 10010
                },
                "enabled": false
            }
        }
    };

    it("initializes", function (done) {
        apimlAuth(pluginDef, pluginConf, serverConf).then(function (authenticator) {
            done();
        }, function (error) {
            assert.fail(error);
        });
    });

    it("authenticates", function (done) {
        this.timeout(10000);
        apimlAuth(pluginDef, pluginConf, serverConf).then(function (authenticator) {
            const request = {
                body: {
                    username: process.env.MF_USERID,
                    password: process.env.MF_PASSWORD
                }
            };
            var sessionState = {};
            authenticator.authenticate(request, sessionState).then(function (response) {
                assert.equal(response.success, true);
                assert.equal(sessionState.authenticated, true);
                assert.ok(sessionState.apimlToken.length > 0);
                done();
            }).catch(function (reason) {
                assert.fail(reason);
            });
        });
    });

    it("reports login failure", function (done) {
        this.timeout(10000);
        apimlAuth(pluginDef, pluginConf, serverConf).then(function (authenticator) {
            const request = {
                body: {
                    username: "wrong",
                    password: "wrong"
                }
            };
            var sessionState = {};
            authenticator.authenticate(request, sessionState).then(function (response) {
                assert.equal(response.success, false);
                assert.equal(sessionState.authenticated, false);
                assert.equal(sessionState.apimlToken, undefined);
                done();
            }).catch(function (reason) {
                assert.fail(reason);
            });
        });
    });
});
