const Octokit = require('octokit').Octokit;
const gatherCiStats = require('./ci').gatherCiStats;
const gatherIssueStats = require('./issues').gatherIssueStats;
const gatherPrStats = require('./pullRequests').gatherPrStats;

// Get the directory with files
const owner = process.argv[2];
const repository = process.argv[3];  //${{ github.repository }} // Would it contain owner or is it separated? But I know these values could be set
const githubToken = process.argv[4];

// CI related
// How long does our pipelines run?

(async function () {

    const octokit = new Octokit({auth: githubToken});

    try {
        await gatherCiStats(octokit, owner, repository);
        await gatherPrStats(octokit, owner, repository);
        // await gatherIssueStats(octokit, owner, repository);
    } catch (e) {
        console.log(e);
    }
})()

