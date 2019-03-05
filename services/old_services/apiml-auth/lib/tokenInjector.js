const express = require('express');

module.exports = pluginContext =
>
{
    const r = express.Router();
    r.get('/**', (req, res) = > {
        const apimlSession = req.session['org.zowe.zlux.auth.apiml'];
    if (apimlSession === undefined) {
        res.status(401).send("Missing APIML authentication token in zLUX session");
    } else {
        const token = apimlSession.apimlToken;
        const gatewayUrl = apimlSession.gatewayUrl;
        const newUrl = gatewayUrl + req.url.replace("1.0.0/", "") + "?apimlAuthenticationToken=" + token;
        res.redirect(newUrl);
    }
})

    return {
        then(f) {
            f(r);
        }
    }
}
;
