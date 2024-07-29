module.exports = {
    extends: ["group:allNonMajor", "config:recommended"],
    dependencyDashboard: true,
    repositories: ['zowe/api-layer'],
    baseBranches: ["v2.x.x","v3.x.x"],
    assignees: [],
    printConfig: true,
    prHourlyLimit: 0, // removes rate limit for PR creation per hour
    recreateWhen: "always", // recreates all closed or blocking PRs not just immortals
    npmrc: "legacy-peer-deps=true", //for updating lock-files
    npmrcMerge: true //be combined with a "global" npmrc
};

// To disable package example:
//> "packageRules": [
//     {
//         "matchPackageNames": ["neutrino"],
//         "enabled": false
//     }
// ]

// Delays creating a PR for x days after detecting a dependency update, but creates the temporary branch right away.
// If the update is removed from the registry within those x days, the bot deletes the branch.
// (to adress situation if author of dependency decide to delete pull)
//> minimumReleaseAge
