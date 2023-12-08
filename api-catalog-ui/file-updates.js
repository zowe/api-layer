const { exec, execSync } = require("child_process");
const { createHash } = require("crypto");
const { readFileSync, writeFile, openSync, existsSync, closeSync } = require("fs");
let hashFileName = "changed-files-hashes";
let fileChangedFlag = "updateflag";
const args = process.argv.slice(2)
execSync("git fetch");
exec("git diff --name-only origin/v3.x.x", (error,stdout,stderr) => {
    const rootDir = args[0];
    if(error) {
        console.log(`error ${error.message}`);
        return;
    }
    if(stderr) {
        console.log(`stderr ${stderr}`);
        return;
    }
    let lines = stdout.split(/\r?\n|\r|\n/g);
    const hashSum = createHash('sha256');
    let updated = false;
    for(let line of lines) {
        if(line.includes("api-catalog-ui/frontend")){
            line = rootDir + "/" + line;
            console.log("File that was updated: " + line);
            let file = readFileSync(line);
            hashSum.update(file);
            updated = true;
        }
    }
    const hex = hashSum.digest("hex");
    hashFileName = rootDir + "/" + hashFileName;
    fileChangedFlag = rootDir + "/" + fileChangedFlag;
    console.log("there was a change: " + updated);
    if(existsSync(hashFileName)){
        console.log("file with hash exists and now it will be compared with previous");
        let hashes = readFileSync(hashFileName, "utf8");
        if(hashes.includes(hex)){
            console.log("there was no recent change");
        } else {
            console.log("there was a recent change and flag will be created");
            writeFile(hashFileName,hex, (err) =>{
                if(err)console.log(err);
            })
            closeSync(openSync(fileChangedFlag,"w",));
        }
    } else if (updated) {
        console.log("file with hash does not exist and flag will be created");
        writeFile(hashFileName,hex, (err) =>{
            if(err)console.log(err);
        })
        closeSync(openSync(fileChangedFlag,"w",));
    }

})
