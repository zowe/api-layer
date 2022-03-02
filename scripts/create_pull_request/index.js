import {Octokit} from "octokit";

const githubToken = process.argv[2];

(async function () {
    const octokit = new Octokit({auth: githubToken});
  
    await octokit.rest.pulls.create({
        owner: 'zowe',
        repo: 'api-layer',
        title: 'Automatic merge to v2 branch',
        head: 'master',
        base: 'v2.x.x',
        body: 'Automatically prepared PR for merging into the V2 branch'
    });
})()

