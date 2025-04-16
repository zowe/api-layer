module.exports = {
    branches: [
        {
            name: "v3.x.x",
            level: "none",
            prerelease: true
        },
        {
            name: "zowe-v?-lts",
            level: "patch"
        }
    ],
    plugins: [
        "@octorelease/changelog",
        "@octorelease/pypi",
        ["@octorelease/github", {
            checkPrLabels: true
        }],
        "@octorelease/git"
    ]
};
