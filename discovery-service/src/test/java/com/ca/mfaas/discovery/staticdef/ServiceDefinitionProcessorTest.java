/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.staticdef;

import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ServiceDefinitionProcessorTest {

    @Test
    public void testProcessServicesDataWithTwoRoutes() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - https://localhost:10019/casamplerestapiservice/\n" +
            "      homePageRelativeUrl: api/v1/pets\n" +
            "      statusPageRelativeUrl: actuator/info\n" +
            "      healthCheckRelativeUrl: actuator/health\n" +
            "      routes:\n" +
            "        - gatewayUrl: api/v1\n" +
            "          serviceRelativeUrl: api/v1\n" +
            "        - gatewayUrl: api/v2\n" +
            "          serviceRelativeUrl: api/v2\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(1, instances.size());
        assertEquals(10019, instances.get(0).getSecurePort());
        assertEquals("CASAMPLERESTAPISERVICE", instances.get(0).getAppName());
        assertEquals("https://localhost:10019/casamplerestapiservice/api/v1/pets", instances.get(0).getHomePageUrl());
        assertEquals("https://localhost:10019/casamplerestapiservice/actuator/health",
            instances.get(0).getSecureHealthCheckUrl());
        assertEquals("https://localhost:10019/casamplerestapiservice/actuator/info",
            instances.get(0).getStatusPageUrl());
        assertEquals("api/v1", instances.get(0).getMetadata().get(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL));
        assertEquals("api/v2", instances.get(0).getMetadata().get(ROUTES + ".api-v2." + ROUTES_GATEWAY_URL));
        assertEquals("/casamplerestapiservice/api/v1",
            instances.get(0).getMetadata().get(ROUTES + ".api-v1." + ROUTES_SERVICE_URL));
        assertEquals("STATIC-localhost:casamplerestapiservice:10019", instances.get(0).getInstanceId());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void testProcessServicesDataWithEmptyHomepage() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYamlEmptyRelativeUrls = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - https://localhost:10019/casamplerestapiservice/\n" +
            "      homePageRelativeUrl: ''\n" +
            "      statusPageRelativeUrl: actuator/info\n" +
            "      healthCheckRelativeUrl: actuator/health\n" +
            "      routes:\n" +
            "        - gatewayUrl: api/v1\n" +
            "          serviceRelativeUrl:\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYamlEmptyRelativeUrls));
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(1, instances.size());
        assertEquals(10019, instances.get(0).getSecurePort());
        assertEquals("CASAMPLERESTAPISERVICE", instances.get(0).getAppName());
        assertEquals("https://localhost:10019/casamplerestapiservice/", instances.get(0).getHomePageUrl());
        assertEquals("https://localhost:10019/casamplerestapiservice/actuator/health",
            instances.get(0).getSecureHealthCheckUrl());
        assertEquals("https://localhost:10019/casamplerestapiservice/actuator/info",
            instances.get(0).getStatusPageUrl());
        assertEquals("api/v1", instances.get(0).getMetadata().get(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL));
        assertEquals("/casamplerestapiservice/",
            instances.get(0).getMetadata().get(ROUTES + ".api-v1." + ROUTES_SERVICE_URL));
        assertEquals("STATIC-localhost:casamplerestapiservice:10019", instances.get(0).getInstanceId());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void testProcessServicesDataNoServicesNode() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();

        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", "something: value"));
        assertEquals(0, result.getInstances().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Error processing file test - Unrecognized field \"something\""));
    }

    @Test
    public void testFileInsteadOfDirectoryForDefinitions() throws URISyntaxException {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();

        List<InstanceInfo> instances = serviceDefinitionProcessor.findServices(
            Paths.get(ClassLoader.getSystemResource("api-defs/staticclient.yml").toURI()).toAbsolutePath().toString());

        assertThat(instances.size(), is(0));
    }

    @Test
    public void testProcessServicesDataWithWrongUrlNoScheme() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - localhost:10019/casamplerestapiservice/\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(0, instances.size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("The URL localhost:10019/casamplerestapiservice/ is malformed"));
    }

    @Test
    public void testProcessServicesDataWithWrongUrlUnsupportedScheme() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - ftp://localhost:10019/casamplerestapiservice/\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(0, instances.size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("The URL ftp://localhost:10019/casamplerestapiservice/ is malformed"));
    }

    @Test
    public void testProcessServicesDataWithWrongUrlMissingHostname() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - https:///casamplerestapiservice/\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(0, instances.size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("The URL https:///casamplerestapiservice/ does not contain a hostname. The instance of casamplerestapiservice will not be created"));
    }

    @Test
    public void testProcessServicesDataWithWrongDocumentationUrl() {

        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - https://localhost:20020/\n" +
            "      apiInfo:\n" +
            "        - apiId: swagger.io.petstore\n" +
            "          gatewayUrl: api/v2\n" +
            "          swaggerUrl: http://localhost:8080/v2/swagger.json\n" +
            "          version: 2.0.0\n" +
            "          documentationUrl: httpBlah://localhost:10021/hellospring/api-doc";

        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));

        List<InstanceInfo> instances = result.getInstances();
        assertEquals(0, instances.size());

        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());
        assertEquals("Metadata creation failed. The instance of casamplerestapiservice will not be created: com.ca.mfaas.exception.MetadataValidationException: The documentation URL \"httpBlah://localhost:10021/hellospring/api-doc\" for service casamplerestapiservice is not valid", result.getErrors().get(0));
    }

    @Test
    public void testProcessServicesDataWithWrongUrlMissingPort() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - https://host/casamplerestapiservice/\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(0, instances.size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("The URL https://host/casamplerestapiservice/ does not contain a port number. The instance of casamplerestapiservice will not be created"));
    }

    @Test
    public void testServiceWithCatalogMetadata() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String yaml =
            "services:\n" +
                "    - serviceId: casamplerestapiservice\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "        - https://localhost:10019/casamplerestapiservice/\n" +
                "catalogUiTiles:\n" +
                "    tileid:\n" +
                "        title: Tile Title\n" +
                "        description: Tile Description\n";

        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", yaml));
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(1, instances.size());
        assertEquals(7, result.getInstances().get(0).getMetadata().size());
    }

    @Test
    public void testCreateInstancesWithUndefinedInstanceBaseUrls() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String yaml =
            "services:\n" +
                "    - serviceId: casamplerestapiservice\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "catalogUiTiles:\n" +
                "    tileid:\n" +
                "        title: Tile Title\n" +
                "        description: Tile Description\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", yaml));
        List<InstanceInfo> instances = result.getInstances();
        assertThat(instances.size(), is(0));
        assertTrue(result.getErrors().get(0).contains("The instanceBaseUrls parameter of casamplerestapiservice is not defined. The instance will not be created."));
    }

    @Test
    public void testCreateInstancesWithUndefinedInstanceBaseUrl() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String yaml =
            "services:\n" +
                "    - serviceId: casamplerestapiservice\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "      - \n" +
                "catalogUiTiles:\n" +
                "    tileid:\n" +
                "        title: Tile Title\n" +
                "        description: Tile Description\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", yaml));
        List<InstanceInfo> instances = result.getInstances();
        assertThat(instances.size(), is(0));
        assertTrue(result.getErrors().get(0).contains("One of the instanceBaseUrl of casamplerestapiservice is not defined. The instance will not be created."));
    }

    @Test
    public void testCreateInstancesWithUndefinedServiceId() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String yaml =
            "services:\n" +
                "    - serviceId: \n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "catalogUiTiles:\n" +
                "    tileid:\n" +
                "        title: Tile Title\n" +
                "        description: Tile Description\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", yaml));
        List<InstanceInfo> instances = result.getInstances();
        assertThat(instances.size(), is(0));
        assertTrue(result.getErrors().get(0).contains("ServiceId is not defined in the file 'test'. The instance will not be created."));
    }

    @Test
    public void testCreateInstancesWithMultipleStaticDefinitions() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String yaml =
            "services:\n" +
                "    - serviceId: casamplerestapiservice\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "       - https://localhost:10012/casamplerestapiservice2\n" +
                "    - serviceId: casamplerestapiservice2\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "    - serviceId: casamplerestapiservice3\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "       - https://localhost:10012/casamplerestapiservice3\n" +
                "catalogUiTiles:\n" +
                "    tileid:\n" +
                "        title: Tile Title\n" +
                "        description: Tile Description\n";

        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", yaml));
        List<InstanceInfo> instances = result.getInstances();
        assertThat(instances.size(), is(2));
        assertTrue(result.getErrors().get(0).contains("The instanceBaseUrls parameter of casamplerestapiservice2 is not defined. The instance will not be created."));

    }

    @Test
    public void testCreateInstancesWithMultipleYmls() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String yaml =
            "services:\n" +
                "    - serviceId: casamplerestapiservice\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "       - https://localhost:10012/casamplerestapiservice\n" +
                "catalogUiTiles:\n" +
                "    tileid:\n" +
                "        title: Tile Title\n" +
                "        description: Tile Description\n";
        String yaml2 =
            "services:\n" +
                "    - serviceId: casamplerestapiservice2\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "        \n" +
                "catalogUiTiles:\n" +
                "    tileid:\n" +
                "        title: Tile Title\n" +
                "        description: Tile Description\n";
        String yaml3 =
            "services:\n" +
                "    - serviceId: casamplerestapiservice3\n" +
                "      title: Title\n" +
                "      description: Description\n" +
                "      catalogUiTileId: tileid\n" +
                "      instanceBaseUrls:\n" +
                "       - https://localhost:10012/casamplerestapiservice3\n" +
                "catalogUiTiles:\n" +
                "    tileid:\n" +
                "        title: Tile Title\n" +
                "        description: Tile Description\n";

        Map<String, String> ymlSources = new HashMap<>();
        ymlSources.put("yaml", yaml);
        ymlSources.put("yaml1", yaml2);
        ymlSources.put("yaml2", yaml3);
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor.processServicesData(ymlSources);
        List<InstanceInfo> instances = result.getInstances();
        assertThat(instances.size(), is(2));
        assertTrue(result.getErrors().get(0).contains("The instanceBaseUrls parameter of casamplerestapiservice2 is not defined. The instance will not be created."));

    }

    @Test
    public void testEnableUnsecurePortIfHttp() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - http://localhost:10019/casamplerestapiservice/\n" +
            "      homePageRelativeUrl: api/v1/pets\n" +
            "      statusPageRelativeUrl: actuator/info\n" +
            "      healthCheckRelativeUrl: actuator/health\n" +
            "      routes:\n" +
            "        - gatewayUrl: api/v1\n" +
            "          serviceRelativeUrl: api/v1\n" +
            "        - gatewayUrl: api/v2\n" +
            "          serviceRelativeUrl: api/v2\n";

        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));
        List<InstanceInfo> instances = result.getInstances();
        assertThat(instances.size(), is(1));
        assertFalse(instances.get(0).isPortEnabled(InstanceInfo.PortType.SECURE));
        assertTrue(instances.get(0).isPortEnabled(InstanceInfo.PortType.UNSECURE));
        assertEquals(instances.get(0).getSecurePort(), instances.get(0).getPort());
        assertEquals(10019, instances.get(0).getSecurePort());

    }

    @Test
    public void shouldGenerateMetadataIfApiInfoIsNotNUll() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      catalogUiTileId: static\n" +
            "      title: Petstore Sample API Service\n" +
            "      description: This is a sample server Petstore REST API service\n" +
            "      instanceBaseUrls:\n" +
            "        - http://localhost:10019\n" +
            "      routes:\n" +
            "        - gatewayUrl: api/v2\n" +
            "          serviceRelativeUrl: /v2\n" +
            "      apiInfo:\n" +
            "        - apiId: swagger.io.petstore\n" +
            "          gatewayUrl: api/v2\n" +
            "          swaggerUrl: http://localhost:8080/v2/swagger.json\n" +
            "          version: 2.0.0\n" +
            "\n" +
            "catalogUiTiles:\n" +
            "    static:\n" +
            "        title: Static API Services\n" +
            "        description: Services which demonstrate how to make an API service discoverable in the APIML ecosystem using YAML definitions\n";

        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(1, instances.size());
        assertEquals(10019, instances.get(0).getSecurePort());
        assertEquals("CASAMPLERESTAPISERVICE", instances.get(0).getAppName());
        assertEquals("api/v2", instances.get(0).getMetadata().get(ROUTES + ".api-v2." + ROUTES_GATEWAY_URL));
        assertEquals("/v2", instances.get(0).getMetadata().get(ROUTES + ".api-v2." + ROUTES_SERVICE_URL));
        assertEquals("static", instances.get(0).getMetadata().get(CATALOG_ID));
        assertEquals("Petstore Sample API Service", instances.get(0).getMetadata().get(SERVICE_TITLE));
        assertEquals("2.0.0", instances.get(0).getMetadata().get(API_INFO + ".api-v2." + API_INFO_VERSION));
        assertEquals("1.0.0", instances.get(0).getMetadata().get(CATALOG_VERSION));
        assertEquals("Static API Services", instances.get(0).getMetadata().get(CATALOG_TITLE));
        assertEquals("http://localhost:8080/v2/swagger.json", instances.get(0).getMetadata().get(API_INFO + ".api-v2." + API_INFO_SWAGGER_URL));
        assertEquals("This is a sample server Petstore REST API service", instances.get(0).getMetadata().get(SERVICE_DESCRIPTION));
        assertEquals("STATIC-localhost:casamplerestapiservice:10019", instances.get(0).getInstanceId());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void shouldGiveErrorIfTileIdIsInvalid() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      catalogUiTileId: adajand\n" +
            "      title: Petstore Sample API Service\n" +
            "      description: This is a sample server Petstore REST API service\n" +
            "      instanceBaseUrls:\n" +
            "        - http://localhost:10019\n" +
            "      routes:\n" +
            "        - gatewayUrl: api/v2\n" +
            "          serviceRelativeUrl: /v2\n" +
            "      apiInfo:\n" +
            "        - apiId: swagger.io.petstore\n" +
            "          gatewayUrl: api/v2\n" +
            "          swaggerUrl: http://localhost:8080/v2/swagger.json\n" +
            "          version: 2.0.0\n" +
            "\n" +
            "catalogUiTiles:\n" +
            "    static:\n" +
            "        title: Static API Services\n" +
            "        description: Services which demonstrate how to make an API service discoverable in the APIML ecosystem using YAML definitions\n";

        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor.processServicesData(Collections.singletonMap("test", routedServiceYaml));
        assertEquals("Error processing file test - The API Catalog UI tile ID adajand is invalid. The service casamplerestapiservice will not have API Catalog UI tile", result.getErrors().get(0));
    }

    @Test
    public void testFindServicesWithTwoDirectories() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String pathOne = ClassLoader.getSystemResource("api-defs/").getPath();
        String pathTwo = ClassLoader.getSystemResource("ext-config/").getPath();
        List<InstanceInfo> instances = serviceDefinitionProcessor.findServices(pathOne + ";" + pathTwo);

        assertThat(instances.size(), is(2));
    }

    @Test
    public void testFindServicesWithOneDirectory() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String pathOne = ClassLoader.getSystemResource("api-defs/").getPath();
        List<InstanceInfo> instances = serviceDefinitionProcessor.findServices(pathOne);

        assertThat(instances.size(), is(1));
    }

    @Test
    public void testFindServicesWithSecondEmptyDirectory() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String pathOne = ClassLoader.getSystemResource("api-defs/").getPath();
        List<InstanceInfo> instances = serviceDefinitionProcessor.findServices(pathOne + ";");

        assertThat(instances.size(), is(1));
    }

    @Test
    public void testProcessServicesDataWithAuthenticationMetadata() {
        ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();
        String routedServiceYaml = "services:\n" +
            "    - serviceId: casamplerestapiservice\n" +
            "      instanceBaseUrls:\n" +
            "        - https://localhost:10019/casamplerestapiservice/\n" +
            "      authentication:\n" +
            "        scheme: httpBasicPassTicket\n" +
            "        applid: TSTAPPL\n";
        ServiceDefinitionProcessor.ProcessServicesDataResult result = serviceDefinitionProcessor
            .processServicesData(Collections.singletonMap("test", routedServiceYaml));
        assertTrue(result.getErrors().isEmpty());
        List<InstanceInfo> instances = result.getInstances();
        assertEquals(1, instances.size());
        assertEquals("httpBasicPassTicket", instances.get(0).getMetadata().get(AUTHENTICATION_SCHEME));
        assertEquals("TSTAPPL", instances.get(0).getMetadata().get(AUTHENTICATION_APPLID));
    }
}

