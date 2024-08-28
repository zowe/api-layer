module.exports = {
    globalExtends: ["config:recommended"], // instead of "extends" this resolve immediately as part of global config
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
            //for v.2.x.x make dashboard approval to all major and minor dependencies updates
            "matchBaseBranches": ["v2.x.x"],
            "matchUpdateTypes": ["major", "minor"],
            "dependencyDashboardApproval": true,
        },
        {
            //for v.3.x.x branch find all packages which are minor and patches,
            // slug them and make PR with name "all non-major dependencies"
            "matchBaseBranches": ["v3.x.x-renovate"],
            "groupName": "all non-major dependencies",
            "groupSlug": "all-minor-patch",
            "matchPackageNames": ["*"],
            "matchUpdateTypes": ["minor", "patch"]
        },
        {
            //for v.3.x.x make dashboard approval to all major dependencies updates
            "matchBaseBranches": ["v3.x.x-renovate"],
            "matchUpdateTypes": ["major"],
            "dependencyDashboardApproval": true,
        }
    ],
    printConfig: true,
    labels: ['dependencies'],
    dependencyDashboardLabels: ['dependencies'],
    commitMessagePrefix: 'chore: ',
    prHourlyLimit: 0, // removes rate limit for PR creation per hour
    npmrc: 'legacy-peer-deps=true', //for updating lock-files
    npmrcMerge: true //be combined with a "global" npmrc
};
