module.exports = {
    branches: [
        {
            name: "reboot/innovation/python_enabler",
            level: "minor"
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
