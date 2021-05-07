const Octokit = require('octokit').Octokit;
const moment = require('moment');

const gatherCiStats = require('./ci').gatherCiStats;
const gatherIssueStats = require('./issues').gatherIssueStats;
const gatherPrStats = require('./pullRequests').gatherPrStats;

const owner = process.argv[2];
const repository = process.argv[3];
const githubToken = process.argv[4];

(async function () {

    const octokit = new Octokit({auth: githubToken});

    try {
        const to = new Date();
        const from = moment().subtract(7, 'days').toDate();

        // Ci stats makes sense once a week
        await gatherCiStats(octokit, owner, repository, from, to);
        // PR stats makes sense once a week
        await gatherPrStats(octokit, owner, repository, from, to);

        const fromIssues = moment().subtract(1, 'month').toDate();
        // Issue stats makes sense once a week but starting from the beginning of year.
        await gatherIssueStats(octokit, owner, repository, fromIssues, to);
    } catch (e) {
        console.log(e);
    }
})()

