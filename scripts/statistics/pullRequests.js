// PR related
// Size of the PR
// Time the PR is opened

async function gatherPrStats(octokit, owner, repo) {
    let pullRequests = await loadPage(octokit, owner, repo, 0);

    let page = 1;
    let time = 0;
    let amountOfPrs = 0;
    while(pullRequests.length > 0) {
        pullRequests
            .forEach(pullRequest => {
                const createdAt = new Date(pullRequest.created_at);
                const closedAt = new Date(pullRequest.closed_at);

                const timeToClose = closedAt - createdAt;
                time += timeToClose;
                amountOfPrs++;
            });

        pullRequests = await loadPage(octokit, owner, repo, page);
        page++;
    }

    const averageTime = time / amountOfPrs;
    const HOUR = 1000 * 60 * 60
    const DAY = 24 * HOUR;
    const days = Math.floor(averageTime / (DAY));
    const hours = Math.floor((averageTime - (days * DAY)) / HOUR);

    console.log({
        averageTime: `Days: ${days}, Hours: ${hours}`
    })
}

async function loadPage (octokit, owner, repo, page) {
    const result = await octokit.rest.pulls.list({
        owner,
        repo,
        per_page: 100,
        state: 'closed',
        page: page
    });

    return result.data;
}



exports.gatherPrStats = gatherPrStats;
