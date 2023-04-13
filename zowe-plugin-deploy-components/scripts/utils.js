const childProcess = require("child_process");
const moment = require("moment");
const core = require("@actions/core");
const exec = require("@actions/exec");

async function execAndGetStderr(commandLine, args, opts={}) {
    const cmdOutput = await exec.getExecOutput(commandLine, args, { ignoreReturnCode: true, ...opts });
    if (cmdOutput.exitCode !== 0) {
        throw new Error(`The command '${commandLine} ${args.join(" ")}' failed with exit code ${cmdOutput.exitCode}\n${cmdOutput.stderr.trim()}`);
    }
}

function getNextVersion(packageName, snapshotDate) {
    snapshotDate = moment.utc(snapshotDate);
    const packageVersions = JSON.parse(childProcess.execSync(`npm view ${packageName} time --json`));
    let latestVersion;
    let latestTime = moment.utc(0);
    for (const [version, time] of Object.entries(packageVersions)) {
        // We give priority to versions that:
        // (1) Include "next" in their name
        // (2) Have a publish date older than or the same as the snapshot date
        // (3) Have the newest publish date that meets the above constraints
        const versionTime = moment.utc(time);
        if (version.includes("next") && versionTime.clone().startOf("day").isSameOrBefore(snapshotDate) && versionTime.isAfter(latestTime)) {
            latestVersion = version;
            latestTime = versionTime;
        }
    }
    return latestVersion || "latest";  // Fall back to "latest" if there is no "next" tag
}

async function getPackageInfo(pkg, opts="", prop="version") {
    core.info(`Getting '${prop}' for package: ${pkg}`);
    const viewArgs = ["view", pkg, prop];
    if (opts) {
        viewArgs.push(opts);
    }
    let cmdOutput;
    try {
        cmdOutput = (await exec.getExecOutput("npm", viewArgs)).stdout.trim();
    } catch {
        throw new Error(`Package not found: ${pkg}`);
    }
    if (cmdOutput.length === 0) {
        throw new Error(`Property not found: ${prop}`);
    }
    core.info(`viewArgs: ${viewArgs}`)
    core.info(`cmdOutput: ${cmdOutput}`)
    return cmdOutput;
}

exports.execAndGetStderr = execAndGetStderr;
exports.getNextVersion = getNextVersion;
exports.getPackageInfo = getPackageInfo;
