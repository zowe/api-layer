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
    npm run test
