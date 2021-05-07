const Octokit = require('octokit').Octokit;
const gatherCiStats = require('./ci').gatherCiStats;
const gatherIssueStats = require('./issues').gatherIssueStats;
const gatherPrStats = require('./pullRequests').gatherPrStats;

const owner = process.argv[2];
const repository = process.argv[3];
const githubToken = process.argv[4];

(async function () {

    const octokit = new Octokit({auth: githubToken});

    try {
        await gatherCiStats(octokit, owner, repository);
        await gatherPrStats(octokit, owner, repository);
        await gatherIssueStats(octokit, owner, repository);
    } catch (e) {
        console.log(e);
    }
})()

