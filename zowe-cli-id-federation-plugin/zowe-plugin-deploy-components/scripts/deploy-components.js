const fs = require("fs");
const os = require("os");
const core = require("@actions/core");
const exec = require("@actions/exec");
const delay = require("delay");

const utils = require(__dirname + "/utils");

const PKG_SCOPE = "@zowe";
const SOURCE_REGISTRY = "https://zowe.jfrog.io/zowe/api/npm/npm-local-release/";
const TARGET_REGISTRY = process.env.NPM_REGISTRY || "https://registry.npmjs.org/";
const TEMP_NPM_TAG = "untagged";
const VIEW_OPTS = `--${PKG_SCOPE}:registry=${SOURCE_REGISTRY}`;
const FAILED_VERSIONS = [];

async function deploy(pkgName, pkgTag) {
    core.info(`📦 Deploying package ${PKG_SCOPE}/${pkgName}@${pkgTag}`);
    fs.rmSync(__dirname + "/../.npmrc", { force: true });
    const pkgVersion = await utils.getPackageInfo(`${PKG_SCOPE}/${pkgName}@${pkgTag}`, VIEW_OPTS);
    let oldPkgVersion;
    try {
        oldPkgVersion = await utils.getPackageInfo(`${PKG_SCOPE}/${pkgName}@${pkgTag}`);
    } catch (err) {
        core.warning(err);  // Do not error out
    }

    if (FAILED_VERSIONS.includes(pkgVersion)) {
        core.warning(`Package ${PKG_SCOPE}/${pkgName}@${pkgVersion} will not be published because it failed to ` +
            `install`);
        return;
    } else if (oldPkgVersion === pkgVersion) {
        core.info(`Package ${PKG_SCOPE}/${pkgName}@${pkgVersion} already exists`);
        return;
    } else if (pkgTag !== pkgVersion ) {
        core.warning(`Package ${PKG_SCOPE}/${pkgName}@${pkgVersion} will not be published because the pkg is  ` +
            `invalid`);
        return;
    }

    try {
        oldPkgVersion = await utils.getPackageInfo(`${PKG_SCOPE}/${pkgName}@${pkgVersion}`);
        core.info(`Package ${PKG_SCOPE}/${pkgName}@${pkgVersion} already exists, adding tag ${pkgTag}`);
        await utils.execAndGetStderr("npm", ["dist-tag", "add", `${PKG_SCOPE}/${pkgName}@${pkgVersion}`, pkgTag]);
    } catch (err) {
        const tgzUrl = await utils.getPackageInfo(`${PKG_SCOPE}/${pkgName}@${pkgTag}`, VIEW_OPTS, "dist.tarball");
        const fullPkgName = `${pkgName}-${pkgVersion}.tgz`;
        await utils.execAndGetStderr("curl", ["-fs", "-o", fullPkgName, tgzUrl]);
        await utils.execAndGetStderr("bash", ["scripts/repackage_tar.sh", fullPkgName, TARGET_REGISTRY, pkgVersion]);
        pkgTag = pkgTag !== pkgVersion ? pkgTag : TEMP_NPM_TAG;
        await utils.execAndGetStderr("npm", ["publish", fullPkgName, "--access", "public", "--tag", "latest"]);
    }

    core.info("Waiting for published version to appear on NPM registry");
    let taggedVersion;
    let versionExists = false;
    while (!versionExists || taggedVersion !== pkgVersion) {
        if (!versionExists) {
            versionExists = (await exec.getExecOutput("npm", ["view", `${PKG_SCOPE}/${pkgName}@${pkgVersion}`,
                "version"], { ignoreReturnCode: true })).stdout.trim().length > 0;
        } else {
            taggedVersion = (await exec.getExecOutput("npm", ["view", `${PKG_SCOPE}/${pkgName}@${pkgTag}`,
                "version"], { ignoreReturnCode: true })).stdout.trim();
        }
        await delay(1000);
    }
    let isUntagged = false;
    if (pkgTag === TEMP_NPM_TAG) {  // Remove temporary npm tag because npm forces us to publish with dist-tag
        isUntagged = true;
        await utils.execAndGetStderr("npm", ["dist-tag", "rm", `${PKG_SCOPE}/${pkgName}`, TEMP_NPM_TAG]);
    }

    core.info("Verifying that deployed package can be installed");
    let installError;
    try {
        await utils.execAndGetStderr("npm", ["install", `${PKG_SCOPE}/${pkgName}@${isUntagged ? pkgVersion : pkgTag}`,
            `--${PKG_SCOPE}:registry=${TARGET_REGISTRY}`], { cwd: fs.mkdtempSync(os.tmpdir() + "/zowe") })
    } catch (err) {
        installError = err;
    }
    if (installError != null) {
        if (oldPkgVersion != null && !isUntagged) {
            core.info(`Install failed, reverting tag ${pkgTag} to v${oldPkgVersion}`);
            await exec.exec("npm", ["dist-tag", "add", `${PKG_SCOPE}/${pkgName}@${oldPkgVersion}`, pkgTag],
                { ignoreReturnCode: true });
        }
        FAILED_VERSIONS.push(pkgVersion);
        throw installError;
    }
}

(async () => {
    const pkgName = process.argv[2];
    const pkgTags = process.argv.slice(3);
    const deployErrors = {};

    for (const pkgTag of pkgTags) {
        try {
            await deploy(pkgName, pkgTag);
        } catch (err) {
            deployErrors[pkgTag] = err;
            core.error(err);
        }
    }

    if (Object.keys(deployErrors).length > 0) {
        let errorReport = "";
        for (const [k, v] of Object.entries(deployErrors)) {
            errorReport += `[${k}] ${v.stack}\n\n`;
        }
        core.setOutput("errors", errorReport.trim());
        core.setFailed(new AggregateError(Object.values(deployErrors)));
        process.exit(1);
    }
})();
