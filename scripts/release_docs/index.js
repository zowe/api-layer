import {Octokit} from "octokit";
import {execSync} from "child_process";
import {readFile, writeFile} from "fs/promises";

const githubToken = process.argv[2];
const version = process.argv[3];
const releaseDate = process.argv[4];
const amountOfVersions = process.argv[5];
const branchToMerge = process.argv[6];
// function records changes, (output is string), conventional-changelog is installed properly bc output is showing.
// last commit in this script is recorded but not the previous commit history, will only filter on that one last commit (if it's not feat or fix, then nothing gets recorded). Shift is not the issue.
// order of changelog is in alphabetical order, with priority fix -> feat -> chore -> no prefix
// gets commits from latest -> last release
// test out if any other command with output works
// maxBuffer 1073741820??
// not getting truncated by maxBuffer (shouldn't), can execute line from above before putting in execSync
// look up execSync and potentially downgrade the conventional-changelog?
(async function () {

    console.log("execSync for changes...manually putting 1")
    try {
        let convChange = `git --no-pager log -n 10`; // testing if this is related to conventional-changelog potentially being truncated. Expected out put should put all the commits.
        const changesTest = execSync(convChange, {maxBuffer: 107374182000});
        console.log("printing out git log changes from execSync: \n" + changesTest)
        const changesTestToString = changesTest.toString();
        console.log("printing out git log changes toString: \n" + changesTestToString)

        console.log("printing out git branch changes...")
        let convChangeBranch = `git branch`;
        const convChangeBranchExecSync = execSync(convChangeBranch, {maxBuffer: 107374182000});
        console.log("printing out git branch changes from execSync: \n" + convChangeBranchExecSync)
        const changesTestBranch = convChangeBranchExecSync.toString();
        console.log("printing out git branch changes toString: \n" + changesTestBranch)


        console.log("printing out pwd changes...")
        let convChangePwd = `pwd`;
        const convChangePwdExecSync = execSync(convChangePwd, {maxBuffer: 107374182000});
        console.log("printing out pwd changes from execSync: \n" + convChangePwdExecSync)
        const changesTestPwd = convChangePwdExecSync.toString();
        console.log("printing out pwd changes toString: \n" + changesTestPwd)



        console.log("git checkout...")
        let convChangeCheckout = `git fetch --unshallow origin apiml/GH2405/fix_gh_workflow && git checkout origin/apiml/GH2405/fix_gh_workflow && git --no-pager log -n 10`;
        const convChangeCheckoutExecSync = execSync(convChangeCheckout, {maxBuffer: 107374182000});
        console.log("printing out checkout changes from execSync: \n" + convChangeCheckoutExecSync)
    } catch (error) {
        console.log("error got thrown")
        console.log(error);
    }

    const changes = execSync(`conventional-changelog -r ${amountOfVersions}`, {encoding: 'utf-8'}).toString();

//
//      const changes = "a\nb\nc"

    const lines = changes.split(/\r?\n/);
    const addedFeatures = lines.filter(line => {
        return line.startsWith("* feat:")
    }).map(line => {
        return line.replace("* feat:", "* Feature: ")
    }).join("\n");
// fixes are here, don't get added
    const addedFixes = lines.filter(line => {
        return line.startsWith("* fix:");
    }).map(line => {
        return line.replace("* fix:", "* Bugfix: ")
    }).join("\n");
// change here
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


Adding changes...

${changes}

Features...

${addedFeatures}

Fixes...

${addedFixes}

${restOfChangelog}`;

    await writeFile('../../CHANGELOG.md', changelogToStore);

    const octokit = new Octokit({auth: githubToken});
    const branch = `apiml/release/${version.replace(/\./g, "_")}`;

    let gitCommitPush = `git checkout ${branch} && git add CHANGELOG.md && git commit --signoff -m "Update changelog try catch" && git push origin ${branch}`;
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
})()

