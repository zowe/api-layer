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
//    const branch = `apiml/release/${version.replace(/\./g, "_")}`;

      // if PR exists (indicate with zowe robot or automatic changelog...), find branch associated with it then checkout that branch and make changes to that
      // else regular process
    console.log("functions for PR data:\n")
    const prs = (await octokit.request("GET /repos/zowe/api-layer/pulls")).data;

    const changelogPrs = prs.filter(pr => pr["user"]["login"] == "zowe-robot" &&
    pr["title"] == "Automatic update for the Changelog for release - Do Not Merge" &&
    pr["body"] == "Update changelog for new release");

//    console.log(changelogPrs);

    if (changelogPrs.length === 1) {
        // PR exists, use that branch to merge new updates
        const prevReleaseBranch = changelogPrs[0]["head"]["ref"];
        console.log("prev release branch is: " + prevReleaseBranch);

        console.log("PRs is 1...");

//        let gitBranch = `git branch`;
//        let gitCheckout = `git fetch origin && git checkout origin/${prevReleaseBranch}`;
//        console.log("my old git branch:\n")
//        console.log(execSync(gitBranch, {
//            cwd: '../../'
//        }).toString());
//
//        execSync(gitCheckout, {
//            cwd: '../../'
//        });
//
//        console.log("my new git branch:\n")
//        console.log(execSync(gitBranch, {
//            cwd: '../../'
//        }).toString());

        let gitCheckoutOrigin = `git fetch origin --quiet && git checkout origin/${prevReleaseBranch}`;

        execSync(gitCheckoutOrigin, {
            cwd: '../../'
        });

        await writeFile('../../CHANGELOG.md', changelogToStore);

//        let gitCommitPush = `git add CHANGELOG.md && git commit --signoff -m "Update changelog" && git push origin HEAD:${prevReleaseBranch}`;
//
//        execSync(gitCommitPush, {
//            cwd: '../../'
//        });



        console.log("git status output...\n");
        let gitStatus = `git status`;

        console.log(execSync(gitStatus, {
            cwd: '../../'
        }).toString());



        console.log("git status porcelain output...\n");
        let gitStatusPorcelain = `git status --porcelain --untracked-files=no`;

        console.log(execSync(gitStatusPorcelain, {
            cwd: '../../'
        }).toString());

        console.log("what's in the changelog?\n");
        let myCommand = `cat CHANGELOG.md`
        console.log(execSync(myCommand, {
            cwd: '../../'
        }).toString());


//        console.log("git add output...\n");
//        let gitAdd = `git add CHANGELOG.md`;
//
//        execSync(gitAdd, {
//            cwd: '../../'
//        });
//
//        console.log("git commit output...\n");
//        let gitCommit = `git commit --signoff -m "Update changelog"`;
//
//        console.log(execSync(gitCommit, {
//            cwd: '../../'
//        })).toString();
//
//
//        console.log("git push output...\n");
//        let gitPush = `git push origin HEAD:${prevReleaseBranch}`;
//        console.log(execSync(gitPush, {
//            cwd: '../../'
//        })).toString();
    }
    else if (changelogPrs.length === 0) {
        // make new PR since none exist for changelog

        await writeFile('../../CHANGELOG.md', changelogToStore);

        const branch = `apiml/release/${version.replace(/\./g, "_")}`;
        console.log("new release branch is: " + branch);

        console.log("PRs is 0...")
        let gitCommitPush = `git branch ${branch} && git checkout ${branch} && git add CHANGELOG.md && git commit --signoff -m "Update changelog" && git push origin ${branch}`;

        execSync(gitCommitPush, {
            cwd: '../../'
        });

        await octokit.rest.pulls.create({
            owner: 'zowe',
            repo: 'api-layer',
            title: 'Automatic update for the Changelog for release - Do Not Merge',
            head: branch,
            base: branchToMerge,
            body: 'Update changelog for new release'
        });
    }
    else {
        throw AssertionError("More than one pull request exists, cannot add new updates to the changelog");
    }


//    const getLatestPRNumber = (data) => data.length === 0 ? 0 : data[0];

//    console.log("awaiting PR data...\n")
//    const data = await getData();
//    const firstPR = getLatestPRNumber(data).toString();


    // uncomment out later
//    console.log("fetch unshallow:\n")
//    let fetch = `git checkout origin/apiml/GH2503/GHA_update_existing_PR && git pull`;
//    const fetchChanges = execSync(fetch).toString();
//    console.log(fetchChanges);
//
//    console.log("check branches:\n")
//    let branchTest = `git branch`;
//    const branchChanges = execSync(branchTest).toString();
//    console.log(branchChanges);

//    console.log("checkout origin for v2.x.x and check branches:\n")
//    let checkoutBranch = `git checkout origin/v2.x.x && git branch`;
//    const checkoutBranchChanges = execSync(checkoutBranch).toString();
//    console.log(checkoutBranchChanges);

    // uncomment out later
//      let gitCommitPush = `git fetch --unshallow origin v2.x.x && git checkout origin/${branch} && git add CHANGELOG.md && git commit --signoff -m "Update changelog" && git push origin ${branch}`;
//      execSync(gitCommitPush, {
//          cwd: '../../'
//      });

        // uncomment out later
  //    await octokit.rest.pulls.create({
  //        owner: 'zowe',
  //        repo: 'api-layer',
  //        title: 'Automatic update for the Changelog for release',
  //        head: branch,
  //        base: branchToMerge,
  //        body: 'Update changelog for new release'
  //    });
})()

