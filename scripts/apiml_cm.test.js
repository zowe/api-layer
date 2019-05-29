const nixt = require("nixt");
const path = require("path");
const fs = require('fs');

var script =  "bash " + path.join(__dirname, "apiml_cm.sh");
script = script.replace(/\\/g,"/");
var testDir = path.join(__dirname, ".apiml_cm_test");
testDir = testDir.replace(/\\/g,"/");
console.log(testDir);

describe(script, function() {
    beforeEach(function(done) {
        if (fs.existsSync(testDir)) {
            nixt()
                .exec(`rm -rf ${testDir}`)
                .mkdir(testDir)
                .run(script)
                .end(done);
        }
        else {
            nixt()
                .mkdir(testDir)
                .run(script)
                .end(done);
        }

    });

    describe("general", function() {
        it("should return usage when there is no argument", function(done) {
            nixt()
                .run(script)
                .code(1)
                .stdout(/usage:/)
                .end(done);
        });
        it("should return usage when there is wrong action", function(done) {
            nixt()
                .run(`${script} --action wrong`)
                .code(1)
                .stdout(/usage:/)
                .stdout(/Called with: --action wrong/)
                .end(done);
        });
    });
    describe("setup", function() {
        it("should create local CA and APIML stores", function(done) {
            this.timeout(60000);
            nixt()
                .cwd(testDir)
                .mkdir(testDir + '/keystore')
                .mkdir(testDir + '/keystore/localhost')
                .mkdir(testDir + '/keystore/local_ca')
                .run(`${script} --action setup`)
                .code(0)
                .exist('keystore/localhost/localhost.keystore.p12')
                .exist('keystore/localhost/localhost.truststore.p12')
                .exist('keystore/localhost/localhost.keystore.cer')
                .exist('keystore/localhost/localhost.keystore.key')
                .exist('keystore/localhost/localhost.keystore.jwtsecret.cer')
                .end(done);
        });
    });
});
