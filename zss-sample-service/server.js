const express = require('express')
const app = express();
app.use(express.json());

const port = 8542;

app.post("/usermap", (req, res) => {
let data = [];
req.on("data", function(chunk) {
    data.push(chunk);
}).on("end", function() {
    var buffer = Buffer.concat(data);
    console.log(buffer);
});
return res.send("user");
});
app.listen(port, () => {
    console.log(`Example app listening at http://localhost:${port}`)
})
