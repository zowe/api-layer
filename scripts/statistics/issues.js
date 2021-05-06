async function gatherIssueStats(octokit, owner, repo) {
    let issues = await loadPage(octokit, owner, repo, 0);

    // while issues are loaded
    const squadMembers = ['balhar-jakub', 'jandadav', 'achmelo', 'CarsonCook', 'jalel01', 'taban03', 'jordanCain'];
    const structured = {
        squad: {
            open: [],
            closed: []
        },
        external: {
            open: [],
            closed: []
        }
    };

    let page = 1;
    while(issues.length > 0) {
        issues
            .filter(issue => !issue.pull_request)
            .forEach(issue => {
                const user = issue.user.login;
                if (squadMembers.indexOf(user) !== -1) {
                    structured["squad"][issue.state].push(issue);
                } else {
                    structured["external"][issue.state].push(issue);
                }
            });

        issues = await loadPage(octokit, owner, repo, page);
        page++;
    }

    const issueStatistics = {
        squad: {
            open: {
                amount: 0,
                averageTime: 0
            },
            closed: {
                amount: 0,
                averageTime: 0
            }
        },
        external: {
            open: {
                amount: 0,
                averageTime: 0
            },
            closed: {
                amount: 0,
                averageTime: 0
            }
        }
    }

    const groups = ['squad', 'external'];
    groups.forEach(group => {
        ['open', 'closed'].forEach(state => {
            const relevantIssues = structured[group][state];
            let time = 0;
            relevantIssues.forEach(issue => {
                const createdAt = new Date(issue.created_at);
                if(issue.state === 'open') {
                    issue.closed_at = new Date();
                }
                const closedAt = new Date(issue.closed_at);

                const timeToClose = closedAt - createdAt;
                time += timeToClose;
            });

            issueStatistics[group][state].amount = relevantIssues.length;
            const averageTime = time / relevantIssues.length;
            // Interested in days and hours.
            const HOUR = 1000 * 60 * 60
            const DAY = 24 * HOUR;
            const days = Math.floor(averageTime / (DAY));
            const hours = Math.floor((averageTime - (days * DAY)) / HOUR);

            issueStatistics[group][state].time = `Days: ${days}, Hours: ${hours}`;
        })
    });

    console.log(issueStatistics);
}

async function loadPage (octokit, owner, repo, page) {
    const result = await octokit.rest.issues.listForRepo({
        owner,
        repo,
        per_page: 100,
        state: 'all',
        page: page
    });

    return result.data;
}

exports.gatherIssueStats = gatherIssueStats;
