const express = require('express')
var fs = require('fs');
var https = require('https');
var privateKey  = fs.readFileSync('../keystore/selfsigned/localhost.keystore.key', 'utf8');
var certificate = fs.readFileSync('../keystore/selfsigned/localhost.keystore.cer', 'utf8');


var credentials = {key: privateKey, cert: certificate};
const app = express();
app.use(express.json());
const port = 8542;

let jsonResponse = {
    "userId" : "CERTSITE",
    "rc": 0,
    "sac_rc": 0,
    "racf_rc": 0,
    "reason_code": 0
}
var httpsServer = https.createServer(credentials, app);
app.post("/certificate/x509/map", (req, res) => {
let data = [];
req.on("data", function(chunk) {
    data.push(chunk);
}).on("end", function() {
    var buffer = Buffer.concat(data);
    console.log(buffer);
});
    return res.send(jsonResponse);
});

httpsServer.listen(port, () => {
    console.log(`Example app listening at http://localhost:${port}`)
})
