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
    public final static String SHUTDOWN = "/application/shutdown";
    public final static String HEALTH = "/application/health";
    public final static String APPLICATIONS = "/eureka/apps";

    public final static String STATIC_WEBSOCKET = "/staticclient/ws/v1";
    public final static String STATIC_UI = "/staticclient/ui/v1";
    public final static String STATIC_REDIRECT = "/staticclient/api/v1/redirect";
    public final static String STATIC_GREET = "/staticclient/api/v1/greeting";

    public final static String ROUTED_AUTH = "/gateway/api/v1/auth";
    public final static String ROUTED_LOGIN = "/gateway/api/v1/auth/login";
    public final static String ROUTED_LOGOUT = "/gateway/api/v1/auth/logout";

    public final static String ROUTED_ZOSMF_AUTH = "/ibmzosmf/api/v1/services/authenticate";

    public final static String ZAAS_TICKET_ENDPOINT = "/gateway/zaas/ticket";
    public final static String ZAAS_ZOSMF_ENDPOINT = "/gateway/zaas/zosmf";
    public final static String ZAAS_ZOWE_ENDPOINT = "/gateway/zaas/zoweJwt";
    public final static String ZAAS_SAFIDT_ENDPOINT = "/gateway/zaas/safIdt";

    public final static String ROUTED_LOGIN_OLD_FORMAT = "/gateway/api/v1/auth/login";
    public final static String ROUTED_LOGOUT_OLD_FORMAT = "/gateway/api/v1/auth/logout";

    public final static String ROUTED_QUERY = "/gateway/api/v1/auth/query";
    public final static String ROUTED_PASSTICKET = "/gateway/api/v1/auth/ticket";
    public final static String ROUTED_SERVICE = "/gateway/api/v1/services";
    public final static String ROUTED_SERVICE_NOT_VERSIONED = "/gateway/services";

    public final static String REVOKE_ACCESS_TOKEN = "/gateway/api/v1/auth/access-token/revoke";
    public static final String REVOKE_ACCESS_TOKENS_FOR_USER = "/gateway/auth/access-token/revoke/tokens/user";
    public static final String REVOKE_OWN_ACCESS_TOKENS = "/gateway/auth/access-token/revoke/tokens";
    public static final String REVOKE_ACCESS_TOKENS_FOR_SCOPE = "/gateway/auth/access-token/revoke/tokens/scope";
    public final static String VALIDATE_ACCESS_TOKEN = "/gateway/api/v1/auth/access-token/validate";
    public final static String GENERATE_ACCESS_TOKEN = "/gateway/api/v1/auth/access-token/generate";
    public final static String EVICT_ACCESS_TOKEN = "/gateway/auth/access-token/evict";

    public final static String VALIDATE_OIDC_TOKEN = "/gateway/api/v1/auth/oidc-token/validate";

    public final static String DISCOVERY_STATIC_API = "/discovery/api/v1/staticApi";

    public final static String CACHING_CACHE = "/cachingservice/api/v1/cache";
    public final static String CACHING_CACHE_LIST = "/cachingservice/api/v1/cache-list";

    public final static String DISCOVERABLE_GREET = "/discoverableclient/api/v1/greeting";
    public final static String DISCOVERABLE_WS_HEADER = "/discoverableclient/ws/v1/header";
    public final static String DISCOVERABLE_WS_UPPERCASE = "/discoverableclient/ws/v1/uppercase";
    public final static String DISCOVERABLE_GET_FILE = "/discoverableclient/api/v1/get-file";
    public final static String DISCOVERABLE_MULTIPART = "/discoverableclient/api/v1/multipart";
    public final static String DISCOVERABLE_SSE_EVENTS = "/discoverableclient/sse/v1/events";
    public final static String X509_ENDPOINT = "/discoverableclient/api/v1/x509";
    public final static String MEDIATION_CLIENT = "/discoverableclient/api/v1/apiMediationClient";

    public final static String PASSTICKET_TEST_ENDPOINT = "/dcpassticket/api/v1/passticketTest";

    public final static String REQUEST_INFO_ENDPOINT = "/dcpassticket/api/v1/request";
    public final static String SAF_IDT_REQUEST = "/dcsafidt/api/v1/request";
    public final static String ZOSMF_REQUEST = "/dczosmf/api/v1/request";
    public final static String ZOWE_JWT_REQUEST = "/zowejwt/api/v1/request";

    public final static String DISCOVERABLE_CLIENT_CONTAINER_ENDPOINT = "/apicatalog/api/v1/containers/cademoapps";
    public final static String DISCOVERABLE_CLIENT_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0";
    public final static String DISCOVERABLE_CLIENT_API_DOC_ENDPOINT_V2 = "/apicatalog/api/v1/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v2.0.0";

    public final static String API_SERVICE_VERSION_DIFF_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0/zowe.apiml.discoverableclient.rest v2.0.0";
    public final static String API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_VERSION = "/apicatalog/api/v1/apidoc/discoverableclient/zowe.apiml.discoverableclient.rest v1.0.0/zowe.apiml.discoverableclient.rest v3.0.0";
    public final static String API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_SERVICE = "/apicatalog/api/v1/apidoc/invalidService/v1/v2";

    public final static String CLOUD_GATEWAY_CERTIFICATES = "/gateway/certificates";

    public final static String ZOSMF_AUTH_ENDPOINT = "/zosmf/services/authenticate";
}
