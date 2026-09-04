/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
/*
 * Copyright 2026 Contributors to the Zowe Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {Octokit} from "octokit";
import {execSync} from "child_process";
import {existsSync} from "fs";
import {writeFile} from "fs/promises";
import {fileURLToPath} from "url";
import {bumpApimlVersions} from "./bump.js";

const githubToken = process.argv[2];
const version = process.argv[3];

const OWNER = 'zowe';
const REPO = 'zowe-install-packaging';
const BRANCH_TO_MERGE = 'v3.x/rc';
const MANIFEST_PATH = 'manifest.json.template';
const LOCAL_CLONE = fileURLToPath(new URL('../../zowe-install-packaging', import.meta.url));
const PR_TITLE = 'Upgrade API ML components for Zowe RC';
const PR_BODY = 'Update manifest.json with bumped API ML components.';
const ROBOT_USER = 'zowe-robot';
const MAX_PR_PAGES = 20;

function git(command) {
    return execSync(command, {cwd: LOCAL_CLONE}).toString();
}

async function listOpenPullRequests(octokit) {
    const perPage = 100;
    const pulls = [];
    for (let page = 1; page <= MAX_PR_PAGES; page++) {
        const {data} = await octokit.request('GET /repos/{owner}/{repo}/pulls', {
            owner: OWNER,
            repo: REPO,
            state: 'open',
            per_page: perPage,
            page,
        });
        pulls.push(...data);
        if (data.length < perPage) {
            return pulls;
        }
    }
    throw new Error(`More than ${MAX_PR_PAGES * perPage} open pull requests in ${OWNER}/${REPO}`);
}

/**
 * Bump the API ML components' versions in the manifest.json.template of
 * zowe-install-packaging and open a pull request against the RC branch, reusing
 * the existing release pull request if one is already open.
 */
(async function () {
    if (!githubToken) {
        throw new Error('Usage: node index.js <github-token> <release-version>');
    }
    if (!existsSync(`${LOCAL_CLONE}/.git`)) {
        throw new Error(`No git clone of ${REPO} at ${LOCAL_CLONE} - ` +
            `clone it into the root of this repository first`);
    }

    const octokit = new Octokit({auth: githubToken});

    const {data} = await octokit.rest.repos.getContent({
        owner: OWNER,
        repo: REPO,
        path: MANIFEST_PATH,
        ref: BRANCH_TO_MERGE,
    });
    const manifestJsonContent = bumpApimlVersions(Buffer.from(data.content, 'base64').toString(), version);

    const pulls = await listOpenPullRequests(octokit);
    const apimlReleasePrs = pulls.filter(pull =>
        pull.user?.login === ROBOT_USER &&
        pull.title === PR_TITLE &&
        pull.base?.ref === BRANCH_TO_MERGE);

    if (apimlReleasePrs.length > 1) {
        const numbers = apimlReleasePrs.map(pull => `#${pull.number}`).join(', ');
        throw new Error(`Found ${apimlReleasePrs.length} open "${PR_TITLE}" pull requests against ` +
            `${BRANCH_TO_MERGE} (${numbers}). Close all but one and re-run.`);
    }

    const existingPr = apimlReleasePrs[0];
    const releaseBranch = existingPr
        ? existingPr.head.ref
        : `apiml/release/${version.replace(/\./g, "_")}`;

    git('git fetch origin --quiet');
    if (existingPr) {
        console.log(`Reusing release branch ${releaseBranch} from #${existingPr.number}`);
        git(`git checkout --quiet origin/${releaseBranch}`);
    } else {
        console.log(`New release branch created ${releaseBranch}`);
        git(`git checkout --quiet -b ${releaseBranch} origin/${BRANCH_TO_MERGE}`);
    }

    await writeFile(`${LOCAL_CLONE}/${MANIFEST_PATH}`, manifestJsonContent);

    if (git('git status --porcelain --untracked-files=no').length === 0) {
        console.log(`No new changes added in manifest.json.template, it already lists ${version}`);
        return;
    }

    console.log(`Pushing updates to ${releaseBranch}`);
    git(`git add ${MANIFEST_PATH} && git commit --signoff -m "Update manifest.json" ` +
        `&& git push origin HEAD:${releaseBranch}`);

    if (existingPr) {
        return;
    }

    const {data: createdPr} = await octokit.rest.pulls.create({
        owner: OWNER,
        repo: REPO,
        title: PR_TITLE,
        head: releaseBranch,
        base: BRANCH_TO_MERGE,
        body: PR_BODY,
    });
    console.log(`Opened ${createdPr.html_url}`);
})().catch(error => {
    console.error(error.message);
    process.exit(1);
});
