package org.zowe.apiml.services;


import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES_GATEWAY_URL;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.ROUTES_SERVICE_URL;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_DESCRIPTION;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_TITLE;

@ExtendWith(MockitoExtension.class)
class BasicInfoServiceTest {

    // Client test configuration
    private final static String CLIENT_SERVICE_ID = "testclient";
    private final static String CLIENT_INSTANCE_ID = CLIENT_SERVICE_ID + ":";
    private final static String CLIENT_HOSTNAME = "client";
    private final static String CLIENT_IP = "192.168.0.1";
    private static final int CLIENT_PORT = 10;
    private static final String CLIENT_HOMEPAGE = "https://client:10";
    private static final String CLIENT_RELATIVE_HEALTH_URL = "/actuator/health";
    private static final String CLIENT_STATUS_URL = "https://client:10/actuator/info";
    private static final String CLIENT_API_ID = "zowe.client.api";
    private static final String CLIENT_API_VERSION = "1.0.0";
    private static final String CLIENT_API_GW_URL = "api/v1";
    private static final boolean CLIENT_API_DEFAULT = true;
    private static final String CLIENT_API_SWAGGER_URL = CLIENT_HOMEPAGE + "/apiDoc";
    private static final String CLIENT_API_DOC_URL = "https://www.zowe.org";
    private static final String CLIENT_ROUTE_UI = "ui/v1";
    private static final String CLIENT_SERVICE_TITLE = "Client service";
    private static final String CLIENT_SERVICE_DESCRIPTION = "Client test service";
    private static final AuthenticationScheme CLIENT_AUTHENTICATION_SCHEME = AuthenticationScheme.ZOSMF;
    private static final String CLIENT_AUTHENTICATION_APPLID = "authid";
    private static final String CLIENT_CUSTOM_METADATA_KEY = "custom.test.key";
    private static final String CLIENT_CUSTOM_METADATA_VALUE = "value";

    // ServiceInfo properties
    private static final String SERVICE_SERVICE_ID = "serviceId";

    @Mock
    private EurekaClient eurekaClient;

    private final EurekaMetadataParser eurekaMetadataParser = new EurekaMetadataParser();

    private BasicInfoService basicInfoService;

    @BeforeEach
    void setUp() {
        basicInfoService = new BasicInfoService(eurekaClient, eurekaMetadataParser);
    }

    @Test
    void whenListingAllServices_thenReturnList() {
        String clientServiceId2 = "testclient2";

        List<Application> applications = Arrays.asList(
                new Application(CLIENT_SERVICE_ID, Collections.singletonList(createFullTestInstance())),
                new Application(clientServiceId2)
        );
        when(eurekaClient.getApplications())
                .thenReturn(new Applications(null, 1L, applications));

        List<ServiceInfo> servicesInfo = basicInfoService.getServicesInfo();


        assertEquals(2, servicesInfo.size());
        assertThat(servicesInfo, contains(
                hasProperty(SERVICE_SERVICE_ID, is(CLIENT_SERVICE_ID)),
                hasProperty(SERVICE_SERVICE_ID, is(clientServiceId2))
        ));
    }

    private InstanceInfo createFullTestInstance() {
        ApiInfo apiInfo = ApiInfo.builder()
                .apiId(CLIENT_API_ID)
                .version(CLIENT_API_VERSION)
                .gatewayUrl(CLIENT_API_GW_URL)
                .isDefaultApi(CLIENT_API_DEFAULT)
                .swaggerUrl(CLIENT_API_SWAGGER_URL)
                .documentationUrl(CLIENT_API_DOC_URL)
                .build();
        Map<String, String> metadata = EurekaMetadataParser.generateMetadata(CLIENT_SERVICE_ID, apiInfo);
        metadata.put(SERVICE_TITLE, CLIENT_SERVICE_TITLE);
        metadata.put(SERVICE_DESCRIPTION, CLIENT_SERVICE_DESCRIPTION);
        metadata.put(ROUTES + ".ui-v1." + ROUTES_SERVICE_URL, "/");
        metadata.put(ROUTES + ".ui-v1." + ROUTES_GATEWAY_URL, CLIENT_ROUTE_UI);
        metadata.put(AUTHENTICATION_SCHEME, CLIENT_AUTHENTICATION_SCHEME.getScheme());
        metadata.put(AUTHENTICATION_APPLID, CLIENT_AUTHENTICATION_APPLID);
        metadata.put(CLIENT_CUSTOM_METADATA_KEY, CLIENT_CUSTOM_METADATA_VALUE);

        return InstanceInfo.Builder.newBuilder()
                .setAppName(CLIENT_SERVICE_ID)
                .setInstanceId(CLIENT_INSTANCE_ID + Math.random())
                .setHostName(CLIENT_HOSTNAME)
                .setIPAddr(CLIENT_IP)
                .enablePort(InstanceInfo.PortType.SECURE, true)
                .setSecurePort(CLIENT_PORT)
                .setHomePageUrl(null, CLIENT_HOMEPAGE)
                .setHealthCheckUrls(CLIENT_RELATIVE_HEALTH_URL, null, null)
                .setStatusPageUrl(null, CLIENT_STATUS_URL)
                .setMetadata(metadata)
                .build();
    }
}