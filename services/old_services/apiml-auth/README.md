# apiml-auth - zLUX Authentication Plugin for Zowe APIML Security Service

It connects to the Zowe APIML Security Service that is available at API gateway.

It contains a "token injector" that injects the _apimlAuthenticationToken_ to an API gateway URL.

For example:
`http://localhost:8543/ZLUX/plugins/org.zowe.zlux.auth.apiml/services/tokenInjector/1.0.0/ui/v1/apicatalog`

goes to the following URL:

`https://localhost:10010/ui/v1/apicatalog/?apimlAuthenticationToken=<token>`

This is used in the IFRAMES in order to obtain the APIML token from the zLUX server-side session.
This token is transformed to the cookie and is not stored in the browser history.

## Testing

It requires APIML Security service to be available at https://localhost:10010.

Run:

    npm install
    MF_USERID=userid MF_PASSWORD="***" npm run test


## Running with zLUX

1. Stand up a local version of the Example Zowe Application Server following instruction at https://zowe.github.io/docs-site/latest/extend/extend-desktop/zlux-example-server.html with having `zlux` superproject at the same level as `api-layer` repository

2. Create `zlux/zowe-app-server/plugins/org.zowe.zlux.auth.apiml.json` with following contents:

    ```json
    {
        "identifier": "org.zowe.zlux.auth.apiml",
        "pluginLocation": "../../../api-layer/apiml-auth"
    }
    ```

3. Edit `zlux/zowe-app-server/config/zluxserver.json` and add to the `"implementationDefaults"` node:

    ```json
    "apiml": {
        "plugins": ["org.zowe.zlux.auth.apiml"]    
    },
    ```

4. Make sure that the APIML port for gateway and discovery service are properly set in `zluxserver.json`
