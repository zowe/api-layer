# metrics-service UI

### Installation of Grafana:
* MacOS: https://grafana.com/docs/grafana/latest/installation/mac/
* Windows: https://grafana.com/docs/grafana/latest/installation/windows/
* Linux: https://grafana.com/docs/grafana/latest/installation/rpm/

### Grafana configuration:
For information regrading the location of grafana configuration see: https://grafana.com/docs/grafana/v7.5/administration/configuration/

To load the custom plugin for http metrics:
1. Uncomment the 'plugins' variable in grafana.ini file and set it to the path to local custom plugin directory, i.e. ```plugin = /path/to/metrics-service-ui/grafana-plugins```
2. Set ```allow_loading_unsigned_plugins = zowe-system-metrics-poc``` in grafana.ini to allow unsigned plugin.
3. run ```npm install``` in the "data-source-plugin" directory to install dependencies
4. run ```npm run-script dev``` to build the plugin.
5. restart grafana server.
   * MacOS: run ```brew services restart grafana```
   * Windows: clicking the  grafana server exe file
   * Linux: ```service grafana-server restart```

### To import the grafana-http dashboard:
1. After loading the custom plugin and restarting the grafana server, go to http://localhost:3000/
2. follow https://grafana.com/docs/grafana/latest/getting-started/getting-started/ for login and changing passwords.
3. go to plugins and create a datasource with the system-metrics-poc plugin
4. follow https://grafana.com/docs/grafana/latest/dashboards/export-import/ to import the dashboard by loading the 'grafana-http-dashboard/APIML-Metrics-Service-HTTP.json' file and choosing system-metrics-poc as datasource.

### To import the grafana-zebra dashboard:
1. go to http://localhost:3000/
2. create a datasource with the "JSON API" plugin, do the following configurations in the settings page:
   In the HTTP/URL field, input: ```https://localhost:10019/metrics-service/zebra/persistent-system-metrics```
   and enable the "Skip TLS Verify" option under the Auth field
3. follow https://grafana.com/docs/grafana/latest/dashboards/export-import/ to import the dashboard by loading the 'grafana-zebra-dashboard/RPRT - CPC Metrics-1649876869207.json' file and choosing JSON API as datasource.

### Troubleshooting
#### CORS Error
For bypassing web security issues, the following actions may be of help:
for CORS related issues, try disabling security for web browsers:
* MAC (Chrome): run ```open -n -a /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --args --user-data-dir="/tmp/chrome_dev_test" --disable-web-security```
* Windows (Chrome): run ```"C:\Program Files\Google\Chrome\Application\chrome.exe" --user-data-dir=“C://Chrome dev session” --disable-web-security``` (change path to chrome.exe to fit local system settings)
* Linux (Chrome): run ```google-chrome --disable-site-isolation-trials --disable-web-security --ignore-certificate-errors --user-data-dir="/tmp"```
#### err_cert_authority_invalid
For ```net::err_cert_authority_invalid``` error, try disabling certificate verifications by:
1. Run the following to ignore certificate errors on google chrom
   * Mac:  ```open -n -a /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --args --user-data-dir="/tmp/chrome_dev_test" --disable-web-security --ignore-certificate-errors```
   * Windows: ```"C:\Program Files\Google\Chrome\Application\chrome.exe" --user-data-dir=“C://Chrome dev session” --disable-web-security --ignore-certificate-errors``` (change path to chrome.exe to fit local system settings)
   * Linux: run ```google-chrome --disable-site-isolation-trials --disable-web-security --ignore-certificate-errors --user-data-dir="/tmp"```
2. Disable certificate verifications for the api-mediation-layer. Set ```verifySslCertificatesOfServices: false``` in ```gateway-service.yml``` and ```discovery-service.yml```.
   Set ```--apiml.security.ssl.verifySslCertificatesOfServices=false``` for gateway-service, discovery-service and api-catalog-service.
3. Set ```app_tls_skip_verify_insecure = true``` under [plugins] in grafana.ini file.

