const fs = require('fs/promises');

async function gatherPrStats(octokit, owner, repo, from) {
    let pullRequests = await loadPage(octokit, owner, repo, 0);

    let page = 1;
    let time = 0;
    let amountOfPrs = 0;
    let timeSince = from;

    let stats = {
        commits: 0,
        additions: 0,
        deletions: 0,
        changed_files: 0,
        averageTime: 0
    }

    const types = ['commits', 'additions', 'deletions', 'changed_files'];
    while(pullRequests.length > 0) {
        for(let i = 0; i < pullRequests.length; i++) {
            let pullRequest = pullRequests[i];

            const createdAt = new Date(pullRequest.created_at);
            const closedAt = new Date(pullRequest.closed_at);
            if(createdAt < timeSince) {
                break;
            }

            const timeToClose = closedAt - createdAt;
            time += timeToClose;
            amountOfPrs++;

            pullRequest.size = await getSizeForPR(octokit, owner, repo, pullRequest.number);

            types.forEach(type => {
                stats[type] += pullRequest.size[type];
            });
        }

        pullRequests = await loadPage(octokit, owner, repo, page);
        page++;
    }

    const averageTime = time / amountOfPrs;
    const HOUR = 1000 * 60 * 60
    const DAY = 24 * HOUR;
    const days = Math.floor(averageTime / (DAY));
    const hours = Math.floor((averageTime - (days * DAY)) / HOUR);

    types.forEach(type => {
        stats[type] /= amountOfPrs;
    });
    stats.averageTime = `Days: ${days}, Hours: ${hours}`;
    stats.amountOfPrs = amountOfPrs;

    await fs.writeFile('pr.json', JSON.stringify(stats));

    console.log(stats);
}

async function getSizeForPR(octokit, owner, repo, prNumber) {
    try {
        const {data} = await octokit.request("GET /repos/{owner}/{repo}/pulls/{number}", {
            owner,
            repo,
            number: prNumber
        });

        return {
            commits: data.commits,
            additions: data.additions,
            deletions: data.deletions,
            changed_files: data.changed_files,
        };
    } catch(e) {
        console.log(e);

        throw e;
    }
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
