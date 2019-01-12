# zlux-api-catalog

This an example of zLUX application plugin that uses an IFRAME to display API Catalog.

You need to create following file `zowe/zlux/zlux-example-server/plugins/org.zowe.api.catalog.json`
in order to include the plugin to your zLUX on desktop:

```json
{
  "identifier": "org.zowe.api.catalog",
  "pluginLocation": "../../../api-layer/zlux-api-catalog"
}
```

We assume that the `api-layer` directory is under `zowe`.
