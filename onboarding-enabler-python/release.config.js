module.exports = {
    branches: [
        {
            name: "main",
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
            // publishRelease: true
        }],
        "@octorelease/git"
    ]
};
