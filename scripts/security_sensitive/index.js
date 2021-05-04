import {Octokit} from "octokit";

// Get the directory with files
const owner = process.argv[2];
const repository = process.argv[3];  //${{ github.repository }} // Would it contain owner or is it separated? But I know these values could be set
const prNumber = process.argv[4]; // ${{ github.event.pull_request.number }}
const githubToken = process.argv[5];

console.log(prNumber);

(async function () {

    const octokit = new Octokit({auth: githubToken});

    const { data } = await octokit.rest.pulls.listFiles({
        owner: owner,
        repo: repository,
        pull_number: prNumber
    });

    let isRisky = false;

    // What do we consider security risky?
    data.forEach(file => {
        if(file.filename.indexOf("security") !== -1) {
            isRisky = true;
        }
    });

    if(isRisky) {
        await octokit.rest.issues.addLabels({
            owner,
            repo: repository,
            issue_number: prNumber,
            labels: ['Sensitive']
        });
    }
})()

