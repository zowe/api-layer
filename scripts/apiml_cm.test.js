var nixt = require("nixt");

let script = 'scripts/apiml_cm.sh';

describe(script, function() {
    describe("general", function() {
        it("should return usage when there is no argument", function(done) {
            nixt()
                .run(script)
                .stdout(/usage:/)
                .end(done);
        });
    });
});
