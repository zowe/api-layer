module.exports = {
    extends: ["group:allNonMajor", "config:recommended"],
    dependencyDashboard: true,
    repositories: ['zowe/api-layer'],
    baseBranches: ["v2.x.x","v3.x.x"],
    printConfig: true,
    prHourlyLimit: 0, // removes rate limit for PR creation per hour
    recreateWhen: "always", // recreates all closed or blocking PRs not just immortals
    npmrc: "legacy-peer-deps=true", //for updating lock-files
    npmrcMerge: true //be combined with a "global" npmrc
};
