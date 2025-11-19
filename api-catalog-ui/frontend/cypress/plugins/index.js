/* eslint-disable header/header */
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

module.exports = (on, config) => {

    on('before:browser:launch', (browser, launchOptions) => {

        if (browser.name === 'chrome') {

            launchOptions.args.push(
                '--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
            );

            launchOptions.args.push('--disable-web-security');
            launchOptions.args.push('--disable-site-isolation-trials');
            launchOptions.args.push('--disable-features=IsolateOrigins,site-per-process');
            launchOptions.args.push('--disable-blink-features=AutomationControlled');

            launchOptions.args.push('--no-sandbox');
            launchOptions.args.push('--disable-gpu');
            launchOptions.args.push('--disable-dev-shm-usage');
        }

        return launchOptions;
    });

    on('task', {
        log(message) {
            console.log('TASK LOG:', message);
            return null;
        }
    });

    return config;
};
