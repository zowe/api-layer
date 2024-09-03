module.exports = {
    globalExtends: ["config:recommended"], // using this instead of "extends" solves the problem with order of the configuration
    repositories: ['zowe/api-layer'],
    baseBranches: ["v2.x.x","v3.x.x"],
    dependencyDashboard: true,
    packageRules: [
        {
            //for v2.x.x branch ignore grouping from extends preset, find all packages which are patches,
            // slug them and make PR with name "all patch dependencies"
            "matchBaseBranches": ["v2.x.x"],
            "groupName": "all patch dependencies",
            "groupSlug": "all-patch",
            "matchPackageNames": ["*"],
            "matchUpdateTypes": ["patch"],
        },
        {
            //for v2.x.x make dashboard approval to all major and minor dependencies updates
            "matchBaseBranches": ["v2.x.x"],
            "matchUpdateTypes": ["major", "minor"],
            "dependencyDashboardApproval": true,
        },
        {
            //for v3.x.x branch find all packages which are minor and patches,
            // slug them and make PR with name "all non-major dependencies"
            "matchBaseBranches": ["v3.x.x"],
            "groupName": "all non-major dependencies",
            "groupSlug": "all-minor-patch",
            "matchPackageNames": ["*"],
            "matchUpdateTypes": ["minor", "patch"]
        },
        {
            //for v3.x.x make dashboard approval to all major dependencies updates
            "matchBaseBranches": ["v3.x.x"],
            "matchUpdateTypes": ["major"],
            "dependencyDashboardApproval": true,
        }
    ],
    printConfig: true,
    labels: ['dependencies'],
    dependencyDashboardLabels: ['dependencies'],
    commitMessagePrefix: 'chore: ',
    prHourlyLimit: 0, // removes rate limit for PR creation per hour
    npmrc: 'legacy-peer-deps=true\nregistry=https://zowe.jfrog.io/artifactory/api/npm/npm-org/', //for updating lock-files
    npmrcMerge: true //be combined with a "global" npmrc
};
