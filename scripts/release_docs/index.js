import {Octokit} from "octokit";
import {execSync} from "child_process";
import {readFile, writeFile} from "fs/promises";

// Get the directory with files
const owner = process.argv[2];
const repository = process.argv[3];  //${{ github.repository }} // Would it contain owner or is it separated? But I know these values could be set
const githubToken = process.argv[4];
const version = '1.22.0 (2021-06-14)';

// Use the version to create a branch name.

(async function () {
    const changes = execSync('conventional-changelog -r 12').toString();

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
    changeLogLines.shift();
    changeLogLines.shift();
    changeLogLines.shift();
    changeLogLines.shift();
    const restOfChangelog = changeLogLines.join("\n");

    const changelogToStore = `# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.

## \`${version}\`

${addedFeatures}


${addedFixes}

${restOfChangelog}`;

    await writeFile('../../CHANGELOG.md', changelogToStore);

    // Create new branch
    // Commit the changes
    // Push the changes
    // Prepare the PR
})()

