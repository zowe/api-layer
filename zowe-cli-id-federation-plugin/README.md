# Zowe CLI Identity Federation Plug-in

## Prerequisites

Before you work with the Zowe CLI sample plug-in, [install Zowe CLI globally.](https://docs.zowe.org/active-development/user-guide/cli-installcli.html)

## Run the Automated Tests

1. `cd __tests__/__resources__/properties`
2. Edit the properties within `custom_properties.yaml` to contain valid system information for your site.
3. `cd` to `zowe-cli-id-federation-plugin` folder
4. `npm run test`

## Install the zowe-cli-id-federation-plugin to Zowe CLI

This process assumes that you already installed Zowe CLI on your PC in the previous steps.

1. `cd` to `zowe-cli-id-federation-plugin` folder.
2. `npm run installPlugin`
3. `zowe idf --help`
   You should see help text displayed if the installation was successful.
