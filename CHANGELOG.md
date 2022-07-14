# API Mediation Layer Changelog

All notable changes to the Zowe API Mediation Layer package will be documented in this file.

## `2.2.3 (2022-07-14)`

* Feature:  revoke PAT by admin (#2476) ([e4d42a9](https://github.com/zowe/api-layer/commit/e4d42a9)), closes [#2476](https://github.com/zowe/api-layer/issues/2476)
* Feature:  Caching Service can store invalidated token rules (#2460) ([055aac9](https://github.com/zowe/api-layer/commit/055aac9)), closes [#2460](https://github.com/zowe/api-layer/issues/2460)
* Feature:  exchange client certificate for SAF IDT (#2455) ([303087c](https://github.com/zowe/api-layer/commit/303087c)), closes [#2455](https://github.com/zowe/api-layer/issues/2455) [#2384](https://github.com/zowe/api-layer/issues/2384)
* Feature:  Fix SAF IDT scheme and service (#2224) ([7772401](https://github.com/zowe/api-layer/commit/7772401)), closes [#2224](https://github.com/zowe/api-layer/issues/2224)
* Feature:  generate personal access token (#2452) ([0e39aa7](https://github.com/zowe/api-layer/commit/0e39aa7)), closes [#2452](https://github.com/zowe/api-layer/issues/2452)
* Feature:  Limit scope of Personal Access Token (#2456) ([cc0aba4](https://github.com/zowe/api-layer/commit/cc0aba4)), closes [#2456](https://github.com/zowe/api-layer/issues/2456)
* Feature:  revoke personal access token (#2422) ([c7f79d5](https://github.com/zowe/api-layer/commit/c7f79d5)), closes [#2422](https://github.com/zowe/api-layer/issues/2422)
* Feature:  Validate ServiceId with Endpoint (#2413) ([9f3825f](https://github.com/zowe/api-layer/commit/9f3825f)), closes [#2413](https://github.com/zowe/api-layer/issues/2413)
* Feature:  Support of expired password in ZAAS client (#2404) ([4eab709](https://github.com/zowe/api-layer/commit/4eab709)), closes [#2404](https://github.com/zowe/api-layer/issues/2404)


* Bugfix:  Optimize image builds (#2445) ([e220cbd](https://github.com/zowe/api-layer/commit/e220cbd)), closes [#2445](https://github.com/zowe/api-layer/issues/2445)
* Bugfix:  recovering after TCP/IP stack was restarted (#2421) ([a851b8f](https://github.com/zowe/api-layer/commit/a851b8f)), closes [#2421](https://github.com/zowe/api-layer/issues/2421)

## `2.1.0 (2022-05-31)`

* Feature:  Add trivial schema files for lib components. Update manifests to remove build metadata (#2379) ([6eba58f](https://github.com/zowe/api-layer/commit/6eba58f)), closes [#2379](https://github.com/zowe/api-layer/issues/2379)
* Feature:  Extend API operation filter in the Swagger UI (#2397) ([cffd6cf](https://github.com/zowe/api-layer/commit/cffd6cf)), closes [#2397](https://github.com/zowe/api-layer/issues/2397)
* Feature:  Generate basic code snippets (#2387) ([79c67d0](https://github.com/zowe/api-layer/commit/79c67d0)), closes [#2387](https://github.com/zowe/api-layer/issues/2387)
* Feature:  New endpoint to retrieve default API doc for service (#2327) ([502ba3c](https://github.com/zowe/api-layer/commit/502ba3c)), closes [#2327](https://github.com/zowe/api-layer/issues/2327)
* Feature:  Enhanced Discovery service health check (#2312) ([2f167ff](https://github.com/zowe/api-layer/commit/2f167ff)), closes [#2312](https://github.com/zowe/api-layer/issues/2312)
* Feature:  Support for TLSv1.3 (#2314) ([e96135a](https://github.com/zowe/api-layer/commit/e96135a)), closes [#2314](https://github.com/zowe/api-layer/issues/2314) [#2269](https://github.com/zowe/api-layer/issues/2269)
* Feature:  Enhance x509 authentication scheme to support client certificates (#2285) ([a053b00](https://github.com/zowe/api-layer/commit/a053b00)), closes [#2285](https://github.com/zowe/api-layer/issues/2285)
* Feature:  Enhance zowejwt authentication scheme to support client certificates (#2292) ([c602080](https://github.com/zowe/api-layer/commit/c602080)), closes [#2292](https://github.com/zowe/api-layer/issues/2292)
* Feature:  Enhance z/OSMF authentication scheme to support client certificates (#2207) ([5750072](https://github.com/zowe/api-layer/commit/5750072)), closes [#2207](https://github.com/zowe/api-layer/issues/2207)
* Feature:  Add support to change password via zOSMF (#2095) ([51e8bd3](https://github.com/zowe/api-layer/commit/51e8bd3)), closes [#2095](https://github.com/zowe/api-layer/issues/2095)
* Feature:  Enable Discovery Service and Gateway Service native library extensions (#1987) ([fd03db5](https://github.com/zowe/api-layer/commit/fd03db5)), closes [#1987](https://github.com/zowe/api-layer/issues/1987)
* Feature:  Add methods for ZaaS client to support password change (#1991) ([7597bd7](https://github.com/zowe/api-layer/commit/7597bd7)), closes [#1991](https://github.com/zowe/api-layer/issues/1991)
* Feature:  API ML sample extension (#1947) ([a085cf3](https://github.com/zowe/api-layer/commit/a085cf3)), closes [#1947](https://github.com/zowe/api-layer/issues/1947)

* Bugfix:  Add log masking class for sensitive logs (#2003) ([994b483](https://github.com/zowe/api-layer/commit/994b483)), closes [#2003](https://github.com/zowe/api-layer/issues/2003)
* Bugfix:  API Catalog swagger link (#2344) ([be07fda](https://github.com/zowe/api-layer/commit/be07fda)), closes [#2344](https://github.com/zowe/api-layer/issues/2344)
* Bugfix:  use same key and record lengths as jcl (#2341) ([d8644f2](https://github.com/zowe/api-layer/commit/d8644f2)), closes [#2341](https://github.com/zowe/api-layer/issues/2341)
* Bugfix:  Add server side logging for swagger handling code (#2328) ([7b0455d](https://github.com/zowe/api-layer/commit/7b0455d)), closes [#2328](https://github.com/zowe/api-layer/issues/2328)
* Bugfix:  Preserve request cookies (#2293) ([71c6649](https://github.com/zowe/api-layer/commit/71c6649)), closes [#2293](https://github.com/zowe/api-layer/issues/2293) [#2269](https://github.com/zowe/api-layer/issues/2269)
* Bugfix:  ZaaS client compatibility with Zowe v2 (#2227) ([abdf995](https://github.com/zowe/api-layer/commit/abdf995)), closes [#2227](https://github.com/zowe/api-layer/issues/2227)
* Bugfix:  Add BearerContent filter to enable bearer auth (#2197) ([1d41704](https://github.com/zowe/api-layer/commit/1d41704)), closes [#2197](https://github.com/zowe/api-layer/issues/2197)
* Bugfix:  Configure southbound timeout with APIML_GATEWAY_TIMEOUT_MILLIS (#2154) ([6af5d6f](https://github.com/zowe/api-layer/commit/6af5d6f)), closes [#2154](https://github.com/zowe/api-layer/issues/2154)
* Bugfix:  Improve error handling for API diff endpoint (#2178) ([1581e39](https://github.com/zowe/api-layer/commit/1581e39)), closes [#2178](https://github.com/zowe/api-layer/issues/2178)
* Bugfix:  Update data model for infinispan storage in Caching service (#2156) ([38a1348](https://github.com/zowe/api-layer/commit/38a1348)), closes [#2156](https://github.com/zowe/api-layer/issues/2156)
* Bugfix:  Versioning in image publishing workflow (#2159) ([db52527](https://github.com/zowe/api-layer/commit/db52527)), closes [#2159](https://github.com/zowe/api-layer/issues/2159)
* Bugfix:  Add x509 auth info to gw api doc (#2142) ([0205470](https://github.com/zowe/api-layer/commit/0205470)), closes [#2142](https://github.com/zowe/api-layer/issues/2142)
* Bugfix:  Properly remove services when instances are removed from Discovery Service (#2128) ([c675b91](https://github.com/zowe/api-layer/commit/c675b91)), closes [#2128](https://github.com/zowe/api-layer/issues/2128)
* Bugfix:  Use ribbon LB for Web sockets (#2147) ([4751dbc](https://github.com/zowe/api-layer/commit/4751dbc)), closes [#2147](https://github.com/zowe/api-layer/issues/2147)
* Bugfix:  Add missing fields in error response (#2118) ([3b9745c](https://github.com/zowe/api-layer/commit/3b9745c)), closes [#2118](https://github.com/zowe/api-layer/issues/2118)
* Bugfix:  Do not require keyAlias for SSL configuration (#2110) ([03bee79](https://github.com/zowe/api-layer/commit/03bee79)), closes [#2110](https://github.com/zowe/api-layer/issues/2110)

## `2.0.7 (2022-03-11)`

* Feature:  Add missing tooltips to all onboarding options (#2194) ([5446fd5](https://github.com/zowe/api-layer/commit/5446fd5)), closes [#2194](https://github.com/zowe/api-layer/issues/2194)
* Feature:  Discovery service can be configured to modify the service ID at registration time (#2229) ([63f6fde](https://github.com/zowe/api-layer/commit/63f6fde)), closes [#2229](https://github.com/zowe/api-layer/issues/2229)
* Feature:  There is now the option to specify base packages for the extensions loader(#2081) ([9a4be5a](https://github.com/zowe/api-layer/commit/9a4be5a)), closes [#2081](https://github.com/zowe/api-layer/issues/2081)
* Feature:  There is a new design of the logout panel in the Catalog dashboard (#2102) ([1382f24](https://github.com/zowe/api-layer/commit/1382f24)), closes [#2102](https://github.com/zowe/api-layer/issues/2102)
* Feature:  There is now the option to change your password via the Catalog UI (#2035) ([139a231](https://github.com/zowe/api-layer/commit/139a231)), closes [#2035](https://github.com/zowe/api-layer/issues/2035)
* Feature:  Migrated the onboarding wizard to the Material UI library. This replaces the Mineral UI framework with the Material UI usage in the wizard (#2004) ([2c595d5](https://github.com/zowe/api-layer/commit/2c595d5)), closes [#2004](https://github.com/zowe/api-layer/issues/2004)
* Feature:  Migrated the dashboard to the Material UI library (#1959) ([0da7f15](https://github.com/zowe/api-layer/commit/0da7f15)), closes [#1959](https://github.com/zowe/api-layer/issues/1959)
* Feature:  Migrated the detail page to the Material UI library (#1949) ([95da488](https://github.com/zowe/api-layer/commit/95da488)), closes [#1949](https://github.com/zowe/api-layer/issues/1949)
* Feature:  Migrated Error components to the Material UI library (#1957) ([c60371d](https://github.com/zowe/api-layer/commit/c60371d)), closes [#1957](https://github.com/zowe/api-layer/issues/1957)
* Feature:  Migrated the header and footer to the Material UI library (#1943) ([537fa34](https://github.com/zowe/api-layer/commit/537fa34)), closes [#1943](https://github.com/zowe/api-layer/issues/1943)
* Feature:  Migrated the Login panel to the Material UI library (#1900) ([81ab2ed](https://github.com/zowe/api-layer/commit/81ab2ed)), closes [#1900](https://github.com/zowe/api-layer/issues/1900) 
* Feature:  Made various improvements to the onboarding wizard (#1772) ([20dd70b](https://github.com/zowe/api-layer/commit/20dd70b)), closes [#1772](https://github.com/zowe/api-layer/issues/1772)

* Bugfix:  Caching service logging (#2222) ([5ff64d9](https://github.com/zowe/api-layer/commit/5ff64d9)), closes [#2222](https://github.com/zowe/api-layer/issues/2222)
* Bugfix:  Upgrade Spring to version without vulnerability (#2252) ([f77fa1b](https://github.com/zowe/api-layer/commit/f77fa1b)), closes [#2252](https://github.com/zowe/api-layer/issues/2252)
* Bugfix:  Use unique artifacts for gha (#2192) ([f8def8c](https://github.com/zowe/api-layer/commit/f8def8c)), closes [#2192](https://github.com/zowe/api-layer/issues/2192)
* Bugfix:  Add x509 auth info to gw api doc (#2142) ([072ad23](https://github.com/zowe/api-layer/commit/072ad23)), closes [#2142](https://github.com/zowe/api-layer/issues/2142)
* Bugfix:  Authorization provider set empty as default (#2107) ([aa77926](https://github.com/zowe/api-layer/commit/aa77926)), closes [#2107](https://github.com/zowe/api-layer/issues/2107)
* Bugfix:  Change url for v2 and mark plugin as v2 in case very old apiml v1 copies don't support same url  ([6f4257a](https://github.com/zowe/api-layer/commit/6f4257a)), closes [#2022](https://github.com/zowe/api-layer/issues/2022)
