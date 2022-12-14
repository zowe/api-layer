const { exec } = require("child_process");
const {
    createHash
} = require("crypto");
const { readFileSync, writeFile, openSync, existsSync, closeSync } = require("fs");
const { dirname, resolve } = require("path");
const hashFileName = "fe-hashcodes";
const fileChangedFlag = "updateflag";
// exec("git fetch; git diff --name-only origin/v2.x.x", (error,stdout,stderr) => {
exec("git diff --name-only origin/v2.x.x", (error,stdout,stderr) => {
    if(error) {
        console.log(`error ${error.message}`);
        return;
    }
    if(stderr) {
        console.log(`stderr ${stderr}`);
        return;
    }
    console.log(`output message: ${stdout}`);
    var lines = stdout.split(/\r?\n|\r|\n/g);
    const hashSum = createHash('sha256');
    for(var line of lines) {
        if(line.includes("api-catalog-ui/frontend")){
            line = line.replace("api-catalog-ui/","");
            console.log("current location: " + dirname(resolve(".")));
            line = dirname(resolve(".")) +"/" + line;
            console.log(line);
            var file = readFileSync(line);
            hashSum.update(file);
        }
    }
    const hex = hashSum.digest("hex");

    if(existsSync(hashFileName)){
        console.log("there was a change, file exists and now it will be compared with previous");
        var hashes = readFileSync(hashFileName, "utf8");
        if(hashes.includes(hex)){
            console.log("there was no new change");
        } else {
            console.log("there was a new change and flag will be created");
            writeFile(hashFileName,hex, (err) =>{
                if(err)console.log(err);
            })
            closeSync(openSync(fileChangedFlag,"w",));
        }
    } else if (hex) {
        console.log("there was a change and file does not exist and flag will be created");
        writeFile(hashFileName,hex, (err) =>{
            if(err)console.log(err);
        })
        closeSync(openSync(fileChangedFlag,"w",));
    }

})
