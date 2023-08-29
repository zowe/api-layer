module.exports = {
    webpack:  function(config, env) {
    // New config, e.g. config.plugins.push...

    config.module.rules = [...config.module.rules,
        {
            resolve: {
                fallback: { "querystring": require.resolve("querystring-es3") }
            }
        }
    ]

    return config
},
    jest: (config) => {
        config.transformIgnorePatterns = ["/node_modules/?!(swagger-ui-react/swagger-ui-es-bundle-core.js)"]
        config.collectCoverageFrom = [
            "src/App.{jsx,js}",
            "src/**/*.{jsx,js}",
            "src/**/reducers/*.{jsx,js}",
            "!src/index.js",
            "!src/responsive-tests/**",
            "!cypress/*"
        ]
        return config;
    }
}



