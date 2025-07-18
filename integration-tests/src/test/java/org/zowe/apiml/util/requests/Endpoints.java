/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.requests;

public class Endpoints {
    public static final String SHUTDOWN = "/application/shutdown";
    public static final String HEALTH = "/application/health";
    public static final String APPLICATIONS = "/eureka/apps";

    public static final String STATIC_WEBSOCKET = "/staticclient/ws/v1";
    public static final String STATIC_UI = "/staticclient/ui/v1";
    public static final String STATIC_REDIRECT = "/staticclient/api/v1/redirect";
    public static final String STATIC_GREET = "/staticclient/api/v1/greeting";

    public static final String ROUTED_AUTH = "/gateway/api/v1/auth";
    public static final String ROUTED_LOGIN = "/gateway/api/v1/auth/login";
    public static final String ROUTED_LOGOUT = "/gateway/api/v1/auth/logout";

    public static final String ROUTED_ZOSMF_AUTH = "/ibmzosmf/api/v1/services/authenticate";

    public static final String ZAAS_TICKET_ENDPOINT = "/zaas/scheme/ticket";
    public static final String ZAAS_ZOSMF_ENDPOINT = "/zaas/scheme/zosmf";
    public static final String ZAAS_ZOWE_ENDPOINT = "/zaas/scheme/zoweJwt";
    public static final String ZAAS_SAFIDT_ENDPOINT = "/zaas/scheme/safIdt";

    public static final String ROUTED_QUERY = "/gateway/api/v1/auth/query";
    public static final String ROUTED_PASSTICKET = "/gateway/api/v1/auth/ticket";
    public static final String ROUTED_SERVICE = "/gateway/api/v1/services";
    public static final String ROUTED_SERVICE_NOT_VERSIONED = "/gateway/services";

    public static final String REVOKE_ACCESS_TOKEN = "/gateway/api/v1/auth/access-token/revoke";
    public static final String REVOKE_ACCESS_TOKENS_FOR_USER = "/gateway/api/v1/auth/access-token/revoke/tokens/user";
    public static final String REVOKE_OWN_ACCESS_TOKENS = "/gateway/api/v1/auth/access-token/revoke/tokens";
    public static final String REVOKE_ACCESS_TOKENS_FOR_SCOPE = "/gateway/api/v1/auth/access-token/revoke/tokens/scope";
    public static final String VALIDATE_ACCESS_TOKEN = "/gateway/api/v1/auth/access-token/validate";
    public static final String GENERATE_ACCESS_TOKEN = "/gateway/api/v1/auth/access-token/generate";
    public static final String EVICT_ACCESS_TOKEN = "/gateway/api/v1/auth/access-token/evict";

    public static final String VALIDATE_OIDC_TOKEN = "/gateway/api/v1/auth/oidc-token/validate";
    public static final String JWK_ALL = "/gateway/api/v1/auth/keys/public/all";

    public static final String SAF_AUTH_CHECK = "/gateway/auth/check";

    public static final String DISCOVERY_STATIC_API = "/discovery/api/v1/staticApi";

    public static final String CACHING_CACHE = "/cachingservice/api/v1/cache";
    public static final String CACHING_CACHE_LIST = "/cachingservice/api/v1/cache-list";

    public static final String DISCOVERABLE_GREET = "/discoverableclient/api/v1/greeting";
    public static final String DISCOVERABLE_WS_HEADER = "/discoverableclient/ws/v1/header";
    public static final String DISCOVERABLE_WS_UPPERCASE = "/discoverableclient/ws/v1/uppercase";
    public static final String DISCOVERABLE_GET_FILE = "/discoverableclient/api/v1/get-file";
    public static final String DISCOVERABLE_MULTIPART = "/discoverableclient/api/v1/multipart";
    public static final String DISCOVERABLE_SSE_EVENTS = "/discoverableclient/sse/v1/events";
    public static final String X509_ENDPOINT = "/discoverableclient/api/v1/x509";
    public static final String MEDIATION_CLIENT = "/discoverableclient/api/v1/apiMediationClient";

    public static final String PASSTICKET_TEST_ENDPOINT = "/dcpassticket/api/v1/passticketTest";

    public static final String REQUEST_INFO_ENDPOINT = "/dcpassticket/api/v1/request";
    public static final String SAF_IDT_REQUEST = "/dcsafidt/api/v1/request";
    public static final String ZOSMF_REQUEST = "/dczosmf/api/v1/request";
    public static final String ZOWE_JWT_REQUEST = "/zowejwt/api/v1/request";

    public static final String DISCOVERABLE_CLIENT_CONTAINER_ENDPOINT = "/apicatalog/api/v1/containers/cademoapps";
    public static final String DISCOVERABLE_CLIENT_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0";
    public static final String DISCOVERABLE_CLIENT_API_DOC_ENDPOINT_V2 = "/apicatalog/api/v1/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v2.0.0";

    public static final String API_SERVICE_VERSION_DIFF_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0/zowe.apiml.discoverableclient.rest v2.0.0";
    public static final String API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_VERSION = "/apicatalog/api/v1/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0/zowe.apiml.discoverableclient.rest v3.0.0";
    public static final String API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_SERVICE = "/apicatalog/api/v1/apidoc/invalidService/v1/v2";

    public static final String CLOUD_GATEWAY_CERTIFICATES = "/gateway/certificates";

    public static final String ZOSMF_AUTH_ENDPOINT = "/zosmf/services/authenticate";
}
