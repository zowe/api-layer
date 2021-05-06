const Octokit = require('octokit').Octokit;
const gatherCiStats = require('./ci').gatherCiStats;
const gatherIssueStats = require('./issues').gatherIssueStats;
const gatherPrStats = require('./pullRequests').gatherPrStats;

// Get the directory with files
const owner = process.argv[2];
const repository = process.argv[3];  //${{ github.repository }} // Would it contain owner or is it separated? But I know these values could be set
const githubToken = process.argv[4];

// Issues related
// Breakdown between issues opened by the squad vs. opened externally
// The time how long we resolve external issues.

// PR related
// Size of the PR
// Time the PR is opened

// CI related
// How long does our pipelines run?

(async function () {

    const octokit = new Octokit({auth: githubToken});

    try {
        gatherCiStats(octokit, owner, repository);
        gatherPrStats(octokit, owner, repository);
        gatherIssueStats(octokit, owner, repository);
        // This works, wew can limit or not limit runs as we see fit.
    } catch (e) {
        console.log(e);
    }
})()

