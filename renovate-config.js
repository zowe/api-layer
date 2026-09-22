module.exports = {
    binarySource: "install",
    globalExtends: ["config:recommended", "helpers:pinGitHubActionDigests"], // using this instead of "extends" solves the problem with order of the configuration
    repositories: ['zowe/api-layer'],
    baseBranches: ["v2.x.x", "v3.x.x"],
    commitBody: "Signed-off-by: {{{gitAuthor}}}",
    dependencyDashboard: true,
    allowedPostUpgradeCommands: ['^npm install'],
    packageRules: [
        {
            //for v2.x.x branch ignore grouping from extends preset, find all packages which are patches,
            // slug them and make PR with name "all patch dependencies"
            "matchBaseBranches": ["v2.x.x"],
            "groupName": "all patch dependencies",
            "groupSlug": "all-patch",
            "matchPackageNames": ["*"],
            "matchUpdateTypes": ["patch"],
            "postUpgradeTasks": {
                "commands": ["npm install --package-lock-only --registry https://zowe.jfrog.io/artifactory/api/npm/npm-org/"],
                "fileFilters": ["**/**"],
                "executionMode": "branch"
            }
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
            "matchUpdateTypes": ["minor", "patch"],
            "postUpgradeTasks": {
                "commands": ["npm install --package-lock-only --registry https://zowe.jfrog.io/artifactory/api/npm/npm-org/"],
                "fileFilters": ["**/**"],
                "executionMode": "branch"
            }
        },
        {
            //for v3.x.x make dashboard approval to all major dependencies updates
            "matchBaseBranches": ["v3.x.x"],
            "matchUpdateTypes": ["major"],
            "dependencyDashboardApproval": true,
        },
        {
            // GitHub Actions are pinned to commit SHAs, so keep those pins current. Digest updates
            // carry updateType "digest"/"pinDigest", which the grouping rules above (patch / minor /
            // major) do not match - without this rule they would arrive as one PR per action.
            "matchManagers": ["github-actions"],
            "matchUpdateTypes": ["pinDigest", "digest"],
            "groupName": "github-actions digests",
            "groupSlug": "github-actions-digests",
        },
        {
            // Reusable workflows are a separate depType ("workflow"). The callers in this repository
            // point at build-conformant-images.yml on the same branch that develops it, so pinning it
            // would leave caller and callee on different revisions and raise a digest PR on every
            // merge to the branch. Keep reusable workflow references on their branch/tag.
            "matchManagers": ["github-actions"],
            "matchDepTypes": ["workflow"],
            "pinDigests": false,
        },
        {
            // The workflow files are pinned on v3.x.x by this change; do not open pinning PRs against
            // the v2.x.x maintenance branch. Delete this rule to extend pinning to v2.x.x as well.
            "matchBaseBranches": ["v2.x.x"],
            "matchManagers": ["github-actions"],
            "pinDigests": false,
        }
    ],
    printConfig: true,
    labels: ['dependencies'],
    dependencyDashboardLabels: ['dependencies'],
    ignoreDeps: ['history', 'jsdom', 'react-router-dom', '@mui/icons-material', '@mui/material', '@material-ui/core', '@material-ui/icons', 'undici'],
    commitMessagePrefix: 'chore: ',
    prHourlyLimit: 0 // removes rate limit for PR creation per hour
};
