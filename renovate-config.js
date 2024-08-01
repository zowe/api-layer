module.exports = {
    extends: ["config:recommended", "group:allNonMajor"],
    dependencyDashboard: true,
    repositories: ['zowe/api-layer'],
    baseBranches: ["v2.x.x","v3.x.x"],
    printConfig: true,
    labels: ["dependencies"],
    commitMessagePrefix:"chore: ",
    prHourlyLimit: 0, // removes rate limit for PR creation per hour
    npmrc: "legacy-peer-deps=true", //for updating lock-files
    npmrcMerge: true //be combined with a "global" npmrc
};

