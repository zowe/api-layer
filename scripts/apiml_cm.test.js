const nixt = require("nixt");
const path = require("path");

const script =  "bash " + path.join(__dirname, "apiml_cm.sh");
const testDir = path.join(__dirname, ".apiml_cm_test");
console.log(testDir);

describe(script, function() {
    beforeEach(function(done) {
        nixt()
            .exec(`rm -Rf ${testDir}`).run(`mkdir ${testDir}`)
            .end(done);
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
                .exec('mkdir -p keystore/local_ca')
                .exec('mkdir -p keystore/localhost')
                .run(`${script} --action setup`)
                .code(0)
                .exist('keystore/localhost/localhost.keystore.p12')
                .exist('keystore/localhost/localhost.truststore.p12')
                .exist('keystore/localhost/localhost.keystore.cer')
                .exist('keystore/localhost/localhost.keystore.key')
                .end(done);
        });
    });
});
