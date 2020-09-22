const express = require('express')
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
app.listen(port, () => {
    console.log(`Example app listening at http://localhost:${port}`)
})
