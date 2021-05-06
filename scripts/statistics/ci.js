async function gatherCiStats(octokit, owner, repository) {
    /*const {data} = await octokit.rest.actions.listWorkflowRuns({
        owner,
        repo: repository,
        workflow_id: 'ci-tests.yml'
    });*/
}

exports.gatherCiStats = gatherCiStats;
