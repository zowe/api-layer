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
    const manifestJsonContent = await octokit.rest.repos.getContent({
        owner: 'zowe',
        repo: 'zowe-install-packaging',
        path: 'manifest.json.template',
        ref: branchToMerge,
    }).split(/\r?\n/);

    const updatedManifestJson = manifestJsonContent.filter(line => {
        line.includes("org.zowe.apiml") && line.notEqual("org.zowe.apiml.cloud-gateway-package") && line.notEqual("common-java-lib-package")
    }).map(line => {
        return line.replace("\"version\": \"(\d+\.\d+\.\d+)\"", `\"version\": \"${version}\"`)
    }).join("\n");

    const prs = (await octokit.request("GET /repos/zowe/zowe-install-packaging/pulls")).data;

    const apimlReleasePrs = prs.filter(pr => pr["user"]["login"] === "zowe-robot" &&
        pr["title"] === `Upgrade API ML components for Zowe ${version}` &&
        pr["body"] === "Update manifest.json with bumped API ML components.");

    if (apimlReleasePrs.length === 1) {
        // PR exists, use that branch to merge new updates

        const prevReleaseBranch = apimlReleasePrs[0]["head"]["ref"];
        let gitCheckoutOrigin = `git fetch origin --quiet && git checkout origin/${prevReleaseBranch}`;

        execSync(gitCheckoutOrigin, {
            cwd: '../../'
        });

        await writeFile('../../manifest.json.template', updatedManifestJson);

        let gitStatusPorcelain = `git status --porcelain --untracked-files=no`;

        let gitStatusPorcelainOutput = execSync(gitStatusPorcelain, {
            cwd: '../../'
        }).toString();

        if (gitStatusPorcelainOutput.length !== 0) {
            console.log("Pushing updates to " + prevReleaseBranch + "\n");
            let gitCommitPush = `git add manifest.json.template && git commit --signoff -m "Update manifest.json" && git push origin HEAD:${prevReleaseBranch}`;

            execSync(gitCommitPush, {
                cwd: '../../'
            });
        }
        else {
            console.log("No new changes added in manifest.json.template");
        }
    }
    else if (apimlReleasePrs.length === 0) {
        // make new PR since none exists for changelog

        await writeFile('../../manifest.json.template', updatedManifestJson);

        const branch = `apiml/release/${version.replace(/\./g, "_")}`;
        console.log("New release branch created " + branch + "\n");

        let gitCommitPush = `git branch ${branch} && git checkout ${branch} && git add manifest.json.template && git commit --signoff -m "Update manifest.json" && git push origin ${branch}`;

        execSync(gitCommitPush, {
            cwd: '../../'
        });

        await octokit.rest.pulls.create({
            owner: 'zowe',
            repo: 'zowe-install-packaging',
            title: `Upgrade API ML components for Zowe ${version}`,
            head: branch,
            base: branchToMerge,
            body: 'Update manifest.json with bumped API ML components.'
        });
    }
    else {
        console.log("More than one pull request exists, cannot add new updates to the manifest.json for API ML components upgrade");
    }

})()

