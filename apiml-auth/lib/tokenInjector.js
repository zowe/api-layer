const express = require('express');

module.exports = pluginContext => {
  const r = express.Router();
  r.get('/**', (req, res) => {
    const token = req.session['org.zowe.zlux.auth.apiml'].apimlToken;
    const gatewayUrl = req.session['org.zowe.zlux.auth.apiml'].gatewayUrl;
    const newUrl = gatewayUrl + req.url + "?apimlAuthenticationToken=" + token;
    console.log(newUrl);
    res.redirect(newUrl);
  })

  return {
    then(f) {
      f(r);
    }
  }
};
