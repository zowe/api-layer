import {Octokit} from "octokit";
import {readFile} from "fs/promises";

// Get the directory with files
const owner = process.argv[2];
const repository = process.argv[3];  //${{ github.repository }} // Would it contain owner or is it separated? But I know these values could be set
const githubToken = process.argv[4];
const prNumber = process.argv[5];

(async function () {

    const octokit = new Octokit({auth: githubToken});

    try {
        const file1 = await readFile("../../docs/docgen/ErrorMessagesDocumentation.md");
        const file2 = await readFile("../../docs-site/docs/troubleshoot/troubleshoot-apiml-error-codes.md");

        if(file1.toString() != file2.toString()) {
            await octokit.rest.issues.addLabels({
                owner,
                repo: repository,
                issue_number: prNumber,
                labels: ['docs']
            });
        }
    } catch (e) {
        console.log(e);
    }
})()

