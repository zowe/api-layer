/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery.staticdef;

import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.util.UrlUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

/**
 * Processes static definition files and creates service instances
 */
@Slf4j
@Component
public class ServiceDefinitionProcessor {

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    private static final String STATIC_INSTANCE_ID_PREFIX = "STATIC-";

    private static final DataCenterInfo DEFAULT_INFO = () -> DataCenterInfo.Name.MyOwn;

    private static final String DEFAULT_TILE_VERSION = "1.0.0";

    private static final YAMLFactory YAML_FACTORY = new YAMLFactory();

    protected List<File> getFiles(StaticRegistrationResult context, String staticApiDefinitionsDirectories) {
        if (StringUtils.isEmpty(staticApiDefinitionsDirectories)) {
            log.info("No static definition directory defined");
            return Collections.emptyList();
        }

        final String[] directories = staticApiDefinitionsDirectories.split(";");
        return Arrays.stream(directories)
            .filter(s -> !s.isEmpty())
            .map(File::new)
            .filter(directory -> {
                final boolean isDir = directory.isDirectory();
                if (isDir) {
                    log.debug("Found directory {}", directory.getPath());
                } else {
                    final Message msg = apimlLog.log("apiml.discovery.staticDefinitionsDirectoryNotValid", directory.getPath());
                    context.getErrors().add(msg);
                }
                return isDir;
            })
            .collect(Collectors.toList());
    }

    /**
     * Creates a list of instances from static definition files
     *
     * @param staticApiDefinitionsDirectories directories containing static definitions
     * @return list of instances
     */
    public StaticRegistrationResult findStaticServicesData(String staticApiDefinitionsDirectories) {
        final StaticRegistrationResult context = new StaticRegistrationResult();

        final List<File> directories = getFiles(context, staticApiDefinitionsDirectories);
        for (final File directory : directories) {
            log.info("Scanning directory with static services definition: " + directory);
            final File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));

            if (files == null) {
                final Message msg = apimlLog.log("apiml.discovery.errorReadingStaticDefinitionFolder", directory.getAbsolutePath());
                context.getErrors().add(msg);
                continue;
            }

            if (files.length == 0) {
                log.info("No static service definition found in directory: {}", directory.getAbsolutePath());
            }

            for (final File file : files) {
                final Definition definition = loadDefinition(context, file);
                if (definition == null) continue;

                process(context, file.getAbsolutePath(), definition);
            }
        }

        return context;
    }

    protected Definition loadDefinition(StaticRegistrationResult context, File file) {
        final String fileName = file.getAbsolutePath();
        log.info("Static API definition file: {}", fileName);

        final String content;
        try {
            content = new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException e) {
            final Message msg = apimlLog.log("apiml.discovery.errorParsingStaticDefinitionFile", fileName);
            context.getErrors().add(msg);
            return null;
        }

        return loadDefinition(context, fileName, content);
    }

    protected Definition loadDefinition(StaticRegistrationResult context, String ymlFileName, String ymlData) {
        final ObjectMapper mapper = new ObjectMapper(YAML_FACTORY);

        try {
            return mapper.readValue(ymlData, Definition.class);
        } catch (IOException e) {
            context.getErrors().add(String.format("Error processing file %s - %s", ymlFileName, e.getMessage()));
        }

        return null;
    }

    protected void process(StaticRegistrationResult context, String ymlFileName, Definition definition) {
        if (definition == null) return;

        // process static services
        Optional.ofNullable(definition.getServices()).ifPresent(
            x -> {
                final Map<String, CatalogUiTile> tiles = Optional.ofNullable(definition.getCatalogUiTiles()).orElse(Collections.emptyMap());
                x.forEach(y -> createInstances(context, ymlFileName, y, tiles));
            }
        );

        // process additional info (to override metadata of services)
        if (definition.getAdditionalServiceMetadata() != null) {
            for (final ServiceOverride so : definition.getAdditionalServiceMetadata()) {
                final Map<String, String> metadata = createMetadata(so, null, null);
                final ServiceOverride.Mode mode = Optional.ofNullable(so.getMode()).orElse(ServiceOverride.Mode.UPDATE);
                final ServiceOverrideData sod = new ServiceOverrideData(mode, metadata);
                if (context.getAdditionalServiceMetadata().put(so.getServiceId(), sod) != null) {
                    context.getErrors().add(String.format("Additional service metadata of %s in processing file %s were replaced for duplicities", so.getServiceId(), ymlFileName));
                }
            }
        }
    }

    private CatalogUiTile getTile(StaticRegistrationResult context, String ymlFileName, Map<String, CatalogUiTile> tiles, Service service) {
        if (service.getCatalogUiTileId() != null) {
            final CatalogUiTile tile = tiles.get(service.getCatalogUiTileId());
            if (tile == null) {
                context.getErrors().add(String.format("Error processing file %s - The API Catalog UI tile ID %s is invalid. The service %s will not have API Catalog UI tile", ymlFileName, service.getCatalogUiTileId(), service.getServiceId()));
            } else {
                tile.setId(service.getCatalogUiTileId());
            }
            return tile;
        }

        return null;
    }

    private List<InstanceInfo> createInstances(StaticRegistrationResult context, String ymlFileName, Service service, Map<String, CatalogUiTile> tiles) {
        try {
            if (service.getServiceId() == null) {
                throw new ServiceDefinitionException(String.format("ServiceId is not defined in the file '%s'. The instance will not be created.", ymlFileName));
            }

            if (service.getInstanceBaseUrls() == null) {
                throw new ServiceDefinitionException(String.format("The instanceBaseUrls parameter of %s is not defined. The instance will not be created.", service.getServiceId()));
            }

            final CatalogUiTile tile = getTile(context, ymlFileName, tiles, service);
            final List<InstanceInfo> output = new ArrayList<>(service.getInstanceBaseUrls().size());
            for (final String instanceBaseUrl : service.getInstanceBaseUrls()) {
                final InstanceInfo instanceInfo = buildInstanceInfo(context, service, tile, instanceBaseUrl);
                if (instanceInfo != null) output.add(instanceInfo);
            }

            return output;
        } catch (ServiceDefinitionException e) {
            context.getErrors().add(e.getMessage());
        }

        return Collections.emptyList();
    }

    private InstanceInfo buildInstanceInfo(StaticRegistrationResult context,
                                           Service service,
                                           CatalogUiTile tile, String instanceBaseUrl) throws ServiceDefinitionException {
        if (instanceBaseUrl == null || instanceBaseUrl.isEmpty()) {
            throw new ServiceDefinitionException(String.format("One of the instanceBaseUrl of %s is not defined. The instance will not be created.", service.getServiceId()));
        }

        String serviceId = service.getServiceId();
        try {
            URL url = new URL(instanceBaseUrl);
            if (url.getHost().isEmpty()) {
                context.getErrors().add(String.format("The URL %s does not contain a hostname. The instance of %s will not be created", instanceBaseUrl, service.getServiceId()));
            } else if (url.getPort() == -1) {
                context.getErrors().add(String.format("The URL %s does not contain a port number. The instance of %s will not be created", instanceBaseUrl, service.getServiceId()));
            } else {
                InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();

                String instanceId = String.format("%s%s:%s:%s", STATIC_INSTANCE_ID_PREFIX, url.getHost(), serviceId, url.getPort());
                String ipAddress = InetAddress.getByName(url.getHost()).getHostAddress();

                setInstanceAttributes(builder, service, instanceId, instanceBaseUrl, url, ipAddress, tile);

                setPort(builder, service, instanceBaseUrl, url);
                log.info("Adding static instance {} for service ID {} mapped to URL {}", instanceId, serviceId,
                    url);

                final InstanceInfo instance = builder.build();
                context.getInstances().add(instance);
                return instance;
            }

            return null;
        } catch (MalformedURLException e) {
            throw new ServiceDefinitionException(String.format("The URL %s is malformed. The instance of %s will not be created: %s",
                instanceBaseUrl, serviceId, e.getMessage()));
        } catch (UnknownHostException e) {
            throw new ServiceDefinitionException(String.format("The hostname of URL %s is unknown. The instance of %s will not be created: %s",
                instanceBaseUrl, serviceId, e.getMessage()));
        } catch (MetadataValidationException mve) {
            throw new ServiceDefinitionException(String.format("Metadata creation failed. The instance of %s will not be created: %s",
                serviceId, mve));
        }
    }

    private void setInstanceAttributes(InstanceInfo.Builder builder,
                                       Service service,
                                       String instanceId, String instanceBaseUrl,
                                       URL url,
                                       String ipAddress,
                                       CatalogUiTile tile) {
        String serviceId = service.getServiceId();

        builder.setAppName(serviceId).setInstanceId(instanceId).setHostName(url.getHost()).setIPAddr(ipAddress)
            .setDataCenterInfo(DEFAULT_INFO).setVIPAddress(serviceId).setSecureVIPAddress(serviceId)
            .setLeaseInfo(LeaseInfo.Builder.newBuilder()
                .setRenewalIntervalInSecs(LeaseInfo.DEFAULT_LEASE_RENEWAL_INTERVAL)
                .setDurationInSecs(LeaseInfo.DEFAULT_LEASE_DURATION).build())
            .setMetadata(createMetadata(service, url, tile));

        if (service.getHomePageRelativeUrl() != null) {
            builder.setHomePageUrl(null, instanceBaseUrl + service.getHomePageRelativeUrl());
        }

        if (service.getStatusPageRelativeUrl() != null) {
            builder.setStatusPageUrl(null, instanceBaseUrl + service.getStatusPageRelativeUrl());
        }
    }

    private void setPort(InstanceInfo.Builder builder,
                         Service service,
                         String instanceBaseUrl,
                         URL url) throws MalformedURLException {
        switch (url.getProtocol()) {
            case "http":
                builder.enablePort(InstanceInfo.PortType.SECURE, false).enablePort(InstanceInfo.PortType.UNSECURE, true)
                    .setPort(url.getPort()).setSecurePort(url.getPort());
                if (service.getHealthCheckRelativeUrl() != null) {
                    builder.setHealthCheckUrls(null, instanceBaseUrl + service.getHealthCheckRelativeUrl(), null);
                }
                break;
            case "https":
                builder.enablePort(InstanceInfo.PortType.SECURE, true).enablePort(InstanceInfo.PortType.UNSECURE, false)
                    .setSecurePort(url.getPort()).setPort(url.getPort());
                if (service.getHealthCheckRelativeUrl() != null) {
                    builder.setHealthCheckUrls(null, null, instanceBaseUrl + service.getHealthCheckRelativeUrl());
                }
                break;
            default:
                throw new MalformedURLException("Invalid protocol");
        }
    }

    private void setMetadataRoutes(Map<String, String> metadata, List<Route> routes, URL url) {
        if (routes == null) return;

        for (final Route rs : routes) {
            String gatewayUrl = UrlUtils.trimSlashes(rs.getGatewayUrl());
            String key = gatewayUrl.replace("/", "-");
            metadata.put(String.format("%s.%s.%s", ROUTES, key, ROUTES_GATEWAY_URL), gatewayUrl);

            if (url != null) {
                String serviceUrl = url.getPath()
                    + (rs.getServiceRelativeUrl() == null ? "" : rs.getServiceRelativeUrl());
                metadata.put(String.format("%s.%s.%s", ROUTES, key, ROUTES_SERVICE_URL), serviceUrl);
            }
        }
    }

    private void setMetadataTile(Map<String, String> metadata, CatalogUiTile tile) {
        if (tile == null) return;

        metadata.put(CATALOG_ID, tile.getId());
        metadata.put(CATALOG_VERSION, DEFAULT_TILE_VERSION);
        metadata.put(CATALOG_TITLE, tile.getTitle());
        metadata.put(CATALOG_DESCRIPTION, tile.getDescription());
    }

    private void setMetadataAppInfo(Map<String, String> metadata, List<ApiInfo> appInfoList, String serviceId) {
        if (appInfoList == null) return;

        for (ApiInfo apiInfo : appInfoList) {
            metadata.putAll(EurekaMetadataParser.generateMetadata(serviceId, apiInfo));
        }
    }

    private void setMetadataAuthentication(Map<String, String> metadata, Authentication authentication) {
        if (authentication == null) return;

        final AuthenticationScheme scheme = authentication.getScheme();
        if (scheme != null) {
            metadata.put(AUTHENTICATION_SCHEME, scheme.toString());
        }

        final String applid = authentication.getApplid();
        if (applid != null) {
            metadata.put(AUTHENTICATION_APPLID, applid);
        }
    }

    private Map<String, String> createMetadata(Service service, URL url, CatalogUiTile tile) {
        final Map<String, String> metadata = new HashMap<>();

        metadata.put(VERSION, CURRENT_VERSION);
        metadata.put(SERVICE_TITLE, service.getTitle());
        metadata.put(SERVICE_DESCRIPTION, service.getDescription());

        setMetadataRoutes(metadata, service.getRoutes(), url);
        setMetadataTile(metadata, tile);
        setMetadataAppInfo(metadata, service.getApiInfo(), service.getServiceId());
        setMetadataAuthentication(metadata, service.getAuthentication());

        return metadata;
    }
}
