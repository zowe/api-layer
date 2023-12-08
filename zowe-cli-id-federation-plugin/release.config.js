//The following configuration is a sample that can be used when releasing the plugin. If it won't be used, it needs to be removed.
module.exports = {
    branches: [
        {
            name: "v3.x.x",
            level: "minor"
        },
        {
            name: "zowe-v?-lts",
            level: "patch"
        }
    ],
    plugins: [
        "@octorelease/changelog",
        ["@octorelease/npm", {
            aliasTags: {
                "latest": ["zowe-v2-lts", "next"]
            },
            smokeTest: true
        }],
        ["@octorelease/github", {
            checkPrLabels: true
        }],
        "@octorelease/git"
    ]
};
