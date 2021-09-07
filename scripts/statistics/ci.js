const fs = require('fs/promises');

async function gatherCiStats(octokit, owner, repo, from, to) {
    // Let's get data for the last month.
    let workflowRuns = await loadPage(octokit, owner, repo, 0);

    let page = 1;
    let timeSince = from;
    let timeTo = to;

    let runTime = 0;
    let amountOfRuns = 0;

    while(workflowRuns.length > 0) {
        for(let i = 0; i < workflowRuns.length; i++) {
            const workflowRun = workflowRuns[i];

            const createdAt = new Date(workflowRun.created_at);
            if(createdAt < timeSince || createdAt > timeTo) {
                break;
            }

            runTime += await loadRunTimeForAction(octokit, owner, repo, workflowRun.id);
            amountOfRuns++;
        }

        workflowRuns = await loadPage(octokit, owner, repo, page);
        page++;
    }

    const stats = {
        averageRunTime: Math.round(runTime / amountOfRuns / 1000),
        amountOfRuns: amountOfRuns
    };

    await fs.writeFile('ci.json', JSON.stringify(stats));
    // Test

    // JSON data
    console.log(stats);
}

async function loadRunTimeForAction(octokit, owner, repo, runId) {
    const response = await octokit.rest.actions.getWorkflowRunUsage({
        owner,
        repo: repo,
        run_id: runId
    });

    return response.data.run_duration_ms;
}

async function loadPage (octokit, owner, repo, page) {
    const result = await octokit.rest.actions.listWorkflowRuns({
        owner,
        repo,
        per_page: 100,
        workflow_id: 'ci-tests.yml',
        page: page,
        status: 'completed'
    });

    return result.data.workflow_runs;
}

exports.gatherCiStats = gatherCiStats;
