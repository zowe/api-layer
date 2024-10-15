import {Octokit} from "octokit";
import {readFile} from "fs/promises";
import {execSync} from "child_process";
const {
    randomBytes,
} = await import('crypto');

// Get the directory with files
const owner = process.argv[2];
const repository = process.argv[3];
const githubToken = process.argv[4];
// replace with short hash
const branchHash = randomBytes(32).toString('hex');

(async function () {

    const octokit = new Octokit({auth: githubToken});

    try {
        const file1 = await readFile("../../docs/docgen/ErrorMessagesDocumentation.md");
        const file2 = await readFile("../../docs/docgen/original-codes.md");

        if (file1.toString() != file2.toString()) {
            const branch = `apiml/pr${branchHash}/changed_errors`;
            let gitNewCode = `git branch apiml/pr${branchHash}/changed_errors && git checkout apiml/pr${branchHash}/changed_errors && cp ../docs/docgen/ErrorMessagesDocumentation.md docs/troubleshoot/troubleshoot-apiml-error-codes.md && git add docs/troubleshoot/troubleshoot-apiml-error-codes.md && git commit --signoff -m "Update error codes" && git push origin apiml/pr${branchHash}/changed_errors`;

            execSync(gitNewCode, {
                cwd: '../../docs-site'
            });

            // Create the PR from this
            const {data} = await octokit.rest.pulls.create({
                owner,
                repo: 'docs-site',
                title: 'Automatic update for the Error messages in API-Layer PR',
                head: branch,
                base: 'master',
                body: 'Updated API ML error messages'
            });

            // To comment on the current PR I need the url so data.url
            await octokit.rest.issues.createComment({
                owner,
                repo: repository,
                issue_number: branchHash,
                body: `The changes to the error code documentation are available in this PR: ${data.html_url}`,
            });

            console.log("Created a PR and updated the changes");
        }
    } catch (e) {
        console.log(e);
    }
})()

