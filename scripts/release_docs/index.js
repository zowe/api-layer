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

    await writeFile('../../CHANGELOG.md', changelogToStore);

    const octokit = new Octokit({auth: githubToken});
    const branch = `apiml/release/${version.replace(/\./g, "_")}`;

      // if PR exists (indicate with zowe robot or automatic changelog...), find branch associated with it then checkout that branch and make changes to that
      // else regular process

    const getData = () => octokit.request('/api-layer/pulls/zowe-robot');
    console.log(getData);



    console.log("fetch unshallow:\n")
    let fetch = `git checkout origin/apiml/GH2503/GHA_update_existing_PR && git pull`;
    const fetchChanges = execSync(fetch).toString();
    console.log(fetchChanges);

    console.log("check branches:\n")
    let branchTest = `git branch`;
    const branchChanges = execSync(branchTest).toString();
    console.log(branchChanges);

//    console.log("checkout origin for v2.x.x and check branches:\n")
//    let checkoutBranch = `git checkout origin/v2.x.x && git branch`;
//    const checkoutBranchChanges = execSync(checkoutBranch).toString();
//    console.log(checkoutBranchChanges);


      let gitCommitPush = `git fetch --unshallow origin v2.x.x && git checkout origin/${branch} && git add CHANGELOG.md && git commit --signoff -m "Update changelog" && git push origin ${branch}`;
      execSync(gitCommitPush, {
          cwd: '../../'
      });

  //    await octokit.rest.pulls.create({
  //        owner: 'zowe',
  //        repo: 'api-layer',
  //        title: 'Automatic update for the Changelog for release',
  //        head: branch,
  //        base: branchToMerge,
  //        body: 'Update changelog for new release'
  //    });
})()

