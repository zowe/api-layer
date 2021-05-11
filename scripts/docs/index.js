import {Octokit} from "octokit";
import {readFile} from "fs/promises";
import {execSync} from "child_process";

// Get the directory with files
const owner = process.argv[2];
const repository = process.argv[3];
const githubToken = process.argv[4];
const prNumber = process.argv[5];

(async function () {

    const octokit = new Octokit({auth: githubToken});

    try {
        const file1 = await readFile("../../docs/docgen/ErrorMessagesDocumentation.md");
        const file2 = await readFile("../../docs/docgen/original-codes.md");

        if (file1.toString() != file2.toString()) {
            await octokit.rest.issues.addLabels({
                owner,
                repo: repository,
                issue_number: prNumber,
                labels: ['docs']
            });

            const branch = `apiml/pr${prNumber}/changed_errors`;
            const pulls = await octokit.rest.pulls.list({
                owner,
                repo: 'docs-site',
                head: `zowe:${branch}`
            });

            let gitExistingCode = `git checkout apiml/pr${prNumber}/changed_errors && git pull origin apiml/pr${prNumber}/changed_errors && cp ../docs/docgen/ErrorMessagesDocumentation.md docs/troubleshoot/troubleshoot-apiml-error-codes.md && git add docs/troubleshoot/troubleshoot-apiml-error-codes.md && git commit --signoff -m "Update error codes" && git push origin apiml/pr${prNumber}/changed_errors`;
            let gitNewCode = `git branch apiml/pr${prNumber}/changed_errors && git checkout apiml/pr${prNumber}/changed_errors && cp ../docs/docgen/ErrorMessagesDocumentation.md docs/troubleshoot/troubleshoot-apiml-error-codes.md && git add docs/troubleshoot/troubleshoot-apiml-error-codes.md && git commit --signoff -m "Update error codes" && git push origin apiml/pr${prNumber}/changed_errors`;

            const isExisting = pulls.data.length > 0;
            if(isExisting) {
                execSync(gitExistingCode, {
                    cwd: '../../docs-site'
                });
            } else {
                execSync(gitNewCode, {
                    cwd: '../../docs-site'
                });

                // Create the PR from this
                const {data} = await octokit.rest.pulls.create({
                    owner,
                    repo: 'docs-site',
                    title: 'Automatic update for the Error messages in API-Layer PR',
                    head: branch,
                    base: 'docs-staging',
                    body: 'Updated API ML error messages'
                });

                // To comment on the current PR I need the url so data.url
                await octokit.rest.issues.createComment({
                    owner,
                    repo: repository,
                    issue_number: prNumber,
                    body: `The changes to the error code documentation are available in this PR: ${data.html_url}`,
                });
            }

            console.log("Created a PR and updated the changes");
        }
    } catch (e) {
        console.log(e);
    }
})()

