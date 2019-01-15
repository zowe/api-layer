/*
  This program and the accompanying materials are
  made available under the terms of the Eclipse Public License v2.0 which accompanies
  this distribution, and is available at https://www.eclipse.org/legal/epl-v20.html
  
  SPDX-License-Identifier: EPL-2.0
  
  Copyright Contributors to the Zowe Project.
*/

const https = require('https');
const fs = require('fs');

function ApimlAuthenticator(pluginDef, pluginConf, serverConf) {
  this.authPluginID = pluginDef.identifier;
  this.pluginConf = pluginConf;
  this.apimlConf = serverConf.node.mediationLayer.server;
  this.gatewayUrl = `https://${this.apimlConf.hostname}:${this.apimlConf.gatewayPort}`;
  if (serverConf.node.https.certificateAuthorities === undefined) {
    console.log("zLUX server is not configured with certificate authorities, APIML authentication plugin will not validate certificates");
    this.httpsAgent = new https.Agent({
      rejectUnauthorized: false
    });
  } else {
    this.httpsAgent = new https.Agent({
      rejectUnauthorized: true,
      ca: readUtf8FilesToArray(serverConf.node.https.certificateAuthorities)
    });
  }
}

function deleteApimlAuthFromSession(sessionState) {
  sessionState.authenticated = false;
  delete sessionState.apimlUsername;
  delete sessionState.apimlToken;
  delete sessionState.apimlCookie;
  delete sessionState.gatewayUrl;
}

function readUtf8FilesToArray(fileArray) {
  var contentArray = [];
  for (var i = 0; i < fileArray.length; i++) {
    const filePath = fileArray[i];
    try {
      var content = fs.readFileSync(filePath);
      if (content.indexOf('-BEGIN CERTIFICATE-') > -1) {
        contentArray.push(content);
      }
      else {
        content = fs.readFileSync(filePath, 'utf8');
        if (content.indexOf('-BEGIN CERTIFICATE-') > -1) {
          contentArray.push(content);
        }
        else {
          console.log('Error: file ' + filePath + 'is not a certificate')
        }
      }
    } catch (e) {
      console.log('Error when reading file=' + filePath + '. Error=' + e.message);
    }
  }

  if (contentArray.length > 0) {
    return contentArray;
  } else {
    return null;
  }
}

ApimlAuthenticator.prototype = {

  getStatus(sessionState) {
    return {
      authenticated: !!sessionState.authenticated,
      username: sessionState.zssUsername
    };
  },

  /**
   * Should be called e.g. when the users enters credentials
   *
   * Supposed to change the state of the client-server session. NOP for
   * stateless authentication (e.g. HTTP basic).
   *
   * `request` must be treated as read-only by the code. `sessionState` is this
   * plugin's private storage within the session (if stateful)
   *
   * If auth doesn't fail, should return an object containing at least
   * { success: true }. Should not reject the promise.
   */
  authenticate(request, sessionState) {
    return new Promise((resolve, reject) => {
      const gatewayUrl = this.gatewayUrl;
      const data = JSON.stringify({
        username: request.body.username,
        password: request.body.password
      });
      const options = {
        hostname: this.apimlConf.hostname,
        port: this.apimlConf.gatewayPort,
        path: '/api/v1/gateway/auth/login',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': data.length
        },
        agent: this.httpsAgent
      }

      const req = https.request(options, (res) => {
        res.on('data', (d) => {
          let apimlCookie;
          if (res.statusCode == 200) {
            if (typeof res.headers['set-cookie'] === 'object') {
              for (const cookie of res.headers['set-cookie']) {
                const content = cookie.split(';')[0];
                if (content.indexOf('apimlAuthenticationToken') >= 0) {
                  apimlCookie = content;
                }
              }
            }
          }

          if (apimlCookie) {
            sessionState.apimlUsername = request.body.username;
            sessionState.authenticated = true;
            sessionState.apimlCookie = apimlCookie;
            sessionState.apimlToken = apimlCookie.split("=")[1];
            sessionState.gatewayUrl = gatewayUrl;
            resolve({ success: true });
          } else {
            deleteApimlAuthFromSession(sessionState);
            resolve({
              success: false,
              error: {
                message: `${res.statusCode} ${res.statusMessage}`
              }
            })
          }
        });
      });

      req.on('error', (error) => {
        console.log("APIML login has failed:");
        console.log(error);
        var details = error.message;
        if ((error.response !== undefined) && (error.response.data !== undefined)) {
          details = error.response.data;
        }
        deleteApimlAuthFromSession(sessionState);
        resolve({
          success: false,
          error: details
        });
      });

      req.write(data);
      req.end();
    });
  },

  /**
   * Invoked for every service call by the middleware.
   *
   * Checks if the session is valid in a stateful scheme, or authenticates the
   * request in a stateless scheme. Then checks if the user can access the
   * resource.  Modifies the request if necessary.
   *
   * `sessionState` is this plugin's private storage within the session (if
   * stateful)
   *
   * The promise should resolve to an object containing, at least,
   * { authorized: true } if everything is fine. Should not reject the promise.
   */
  authorized(request, sessionState) {
    if (sessionState.authenticated) {
      request.username = sessionState.apimlUsername;
      request.apimlToken = sessionState.apimlToken;
      return Promise.resolve({ authenticated: true, authorized: true });
    } else {
      return Promise.resolve({ authenticated: false, authorized: false });
    }
  },

  addProxyAuthorizations(req1, req2Options, sessionState) {
    if (!sessionState.apimlCookie) {
      return;
    }
    req2Options.headers['apimlToken'] = sessionState.apimlToken;
  }
};

module.exports = function (pluginDef, pluginConf, serverConf) {
  return Promise.resolve(new ApimlAuthenticator(pluginDef, pluginConf, serverConf));
}
