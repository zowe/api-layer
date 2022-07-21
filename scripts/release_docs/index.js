import {Octokit} from "octokit";
import {execSync} from "child_process";
import {readFile, writeFile} from "fs/promises";

const githubToken = process.argv[2];
const version = process.argv[3];
const releaseDate = process.argv[4];
const amountOfVersions = process.argv[5];
const branchToMerge = process.argv[6];

(async function () {
    let convChangeLog = `git fetch --unshallow --tags origin ${branchToMerge} --quiet && git checkout origin/${branchToMerge} --quiet && conventional-changelog -r ${amountOfVersions}`;

    const changes = execSync(convChangeLog).toString();

    const lines = changes.split(/\r?\n/);
    const addedFeatures = lines.filter(line => {
        return line.startsWith("* feat:")
    }).map(line => {
        return line.replace("* feat:", "* Feature: ")
    }).join("\n");

    const addedFixes = lines.filter(line => {
        return line.startsWith("* fix:");
    }).map(line => {
        return line.replace("* fix:", "* Bugfix: ")
    }).join("\n");

    const currentChangelog = await readFile("../../CHANGELOG.md");
    const changeLogLines = currentChangelog.toString().split(/\r?\n/)
    // Remove first 4 lines as they will be replaces by the header that\s visible below
    changeLogLines.shift();
    changeLogLines.shift();
    changeLogLines.shift();
    changeLogLines.shift();
    const restOfChangelog = changeLogLines.join("\n");

    const changelogToStore = `# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.

## \`${version} (${releaseDate})\`

${addedFeatures}


${addedFixes}

${restOfChangelog}`;

    const octokit = new Octokit({auth: githubToken});

    const prs = (await octokit.request("GET /repos/zowe/api-layer/pulls")).data;

    const changelogPrs = prs.filter(pr => pr["user"]["login"] == "zowe-robot" &&
    pr["title"] == "Automatic update for the Changelog for release" &&
    pr["body"] == "Update changelog for new release");

    if (changelogPrs.length === 1) {
        // PR exists, use that branch to merge new updates
        const prevReleaseBranch = changelogPrs[0]["head"]["ref"];
        let gitCheckoutOrigin = `git fetch origin ${branchToMerge} --quiet && git checkout origin/${prevReleaseBranch}`;

        execSync(gitCheckoutOrigin, {
            cwd: '../../'
        });

        await writeFile('../../CHANGELOG.md', changelogToStore);

        let gitStatusPorcelain = `git status --porcelain --untracked-files=no`;

        let gitStatusPorcelainOutput = execSync(gitStatusPorcelain, {
            cwd: '../../'
        }).toString();

        if (gitStatusPorcelainOutput.length != 0) {
            console.log("Pushing updates to " + prevReleaseBranch + "\n");
            let gitCommitPush = `git add CHANGELOG.md && git commit --signoff -m "Update changelog" && git push origin HEAD:${prevReleaseBranch}`;

            execSync(gitCommitPush, {
                cwd: '../../'
            });
        }
        else {
            console.log("No new changes added in CHANGELOG.md");
        }
    }
    else if (changelogPrs.length === 0) {
        // make new PR since none exists for changelog

        await writeFile('../../CHANGELOG.md', changelogToStore);

        const branch = `apiml/release/${version.replace(/\./g, "_")}`;
        console.log("New release branch created " + branch + "\n");

        let gitCommitPush = `git fetch origin ${branchToMerge} && git checkout -b origin/${branch} && git add CHANGELOG.md && git commit --signoff -m "Update changelog" && git push origin HEAD:${branch}`;

        execSync(gitCommitPush, {
            cwd: '../../'
        });

        await octokit.rest.pulls.create({
            owner: 'zowe',
            repo: 'api-layer',
            title: 'Automatic update for the Changelog for release',
            head: branch,
            base: branchToMerge,
            body: 'Update changelog for new release'
        });
    }
    else {
        console.log("More than one pull request exists, cannot add new updates to the changelog");
    }

})()

