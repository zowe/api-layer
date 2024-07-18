import {Octokit} from "octokit";
import {execSync} from "child_process";
import {writeFile} from "fs/promises";

const githubToken = process.argv[2];
const version = process.argv[3];

/**
 * Create and check out release branch from zowe-install-packaging,
 * bump API ML components' version in the manifest.json.template file and open PR against RC base branch, if not open already.
 */
(async function () {
    const branchToMerge = 'v2.x/rc'
    const octokit = new Octokit({auth: githubToken});
    // Get manifest.json.template from the release branch

    let {data}  = await octokit.rest.repos.getContent({
        owner: 'zowe',
        repo: 'zowe-install-packaging',
        path: 'manifest.json.template',
        ref: branchToMerge,
    });
    let manifestJsonContent = Buffer.from(data.content, 'base64').toString();
    manifestJsonContent = JSON.parse(manifestJsonContent)

    Object.entries(manifestJsonContent.binaryDependencies).forEach(([key, value]) => {
        if (key.includes("org.zowe.apiml") && !key.includes("org.zowe.apiml.gateway-package") && !key.includes("common-java-lib-package")) {
            value.version = value.version.replace(value.version, `${version}`);
        }
    });

    Object.entries(manifestJsonContent.sourceDependencies[0]).forEach(([key, value]) => {
        if (key === "Zowe API Mediation Layer") {
            value.entries[0].tag = value.entries[0].tag.replace(value.entries[0].tag, `v${version}`);
        }
    });

    Object.entries(manifestJsonContent.imageDependencies).forEach(([key, value]) => {
        if (key === "api-catalog" || key === "caching" || key === "discovery" || key === "gateway") {
            value.tag = value.tag.replace(value.tag, `${version}-ubuntu`);
        }
    });
    const prs = (await octokit.request("GET /repos/zowe/zowe-install-packaging/pulls")).data;

    const apimlReleasePrs = prs.filter(pr => pr["user"]["login"] === "zowe-robot" &&
        pr["title"] === "Upgrade API ML components for Zowe RC" &&
        pr["body"] === "Update manifest.json with bumped API ML components.");

    if (apimlReleasePrs.length === 1) {
        // PR exists, use that branch to merge new updates

        const prevReleaseBranch = apimlReleasePrs[0]["head"]["ref"];
        let gitCheckoutOrigin = `git fetch origin --quiet && git checkout origin/${prevReleaseBranch}`;

        execSync(gitCheckoutOrigin, {
            cwd: '../../zowe-install-packaging'
        });

        await writeFile('../../zowe-install-packaging/manifest.json.template', JSON.stringify(manifestJsonContent, null, 2));

        let gitStatusPorcelain = `git status --porcelain --untracked-files=no`;

        let gitStatusPorcelainOutput = execSync(gitStatusPorcelain, {
            cwd: '../../zowe-install-packaging'
        }).toString();

        if (gitStatusPorcelainOutput.length !== 0) {
            console.log("Pushing updates to " + prevReleaseBranch + "\n");
            let gitCommitPush = `git add manifest.json.template && git commit --signoff -m "Update manifest.json" && git push origin HEAD:${prevReleaseBranch}`;

            execSync(gitCommitPush, {
                cwd: '../../zowe-install-packaging'
            });
        }
        else {
            console.log("No new changes added in manifest.json.template");
        }
    }
    else if (apimlReleasePrs.length === 0) {
        // make new PR since none exists for components upgrade

        await writeFile('../../zowe-install-packaging/manifest.json.template', JSON.stringify(manifestJsonContent, null, 2));

        const branch = `apiml/release/${version.replace(/\./g, "_")}`;
        console.log("New release branch created " + branch + "\n");

        let gitCommitPush = `git branch ${branch} && git checkout ${branch} && git add manifest.json.template && git commit --signoff -m "Update manifest.json" && git push origin ${branch}`;

        execSync(gitCommitPush, {
            cwd: '../../zowe-install-packaging'
        });

        await octokit.rest.pulls.create({
            owner: 'zowe',
            repo: 'zowe-install-packaging',
            title: `Upgrade API ML components for Zowe RC`,
            head: branch,
            base: branchToMerge,
            body: 'Update manifest.json with bumped API ML components.'
        });
    }
    else {
        console.log("More than one pull request exists, cannot add new updates to the manifest.json for API ML components upgrade");
    }

})()

