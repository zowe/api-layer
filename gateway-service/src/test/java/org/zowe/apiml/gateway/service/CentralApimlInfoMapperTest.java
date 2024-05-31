/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service;


import com.netflix.appinfo.InstanceInfo;
import org.apache.groovy.util.Maps;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.gateway.service.model.ApimlInfo;
import org.zowe.apiml.gateway.service.model.CentralServiceInfo;
import org.zowe.apiml.services.ServiceInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CentralApimlInfoMapperTest {

    @InjectMocks
    private CentralApimlInfoMapper centralApimlInfoMapper;

    @Nested
    class WhenParametersAreInvalid {
        @Test
        void shouldThrowNPEWhenApimlIdIsNull() {
            List<ServiceInfo> serviceInfoList = new ArrayList<>();
            assertThatThrownBy(() -> centralApimlInfoMapper.buildApimlServiceInfo(null, serviceInfoList))
                    .isExactlyInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldReturnEmptyServicesWhenServiceListIsNull() {
            ApimlInfo info = centralApimlInfoMapper.buildApimlServiceInfo("apiml1", null);

            assertThat(info.getApimlId()).isEqualTo("apiml1");
            assertThat(info.getServices()).isEmpty();
        }

        @Test
        void shouldReturnEmptyServicesWhenServiceListContainsNulls() {
            ApimlInfo info = centralApimlInfoMapper.buildApimlServiceInfo("apiml1", asList(null, null, null));

            assertThat(info.getApimlId()).isEqualTo("apiml1");
            assertThat(info.getServices()).isEmpty();
        }
    }

    @Nested
    class WhenMappingServiceInfo {

        private ServiceInfo serviceOne;
        private ServiceInfo.Apiml apiml;
        private ServiceInfo.ApiInfoExtended apiInfo;

        @BeforeEach
        public void setUp() {
            centralApimlInfoMapper.metadataKeysAllowList = Sets.set("zos.sysname", "zos.system", "zos.sysplex", "zos.cpcName", "zos.zosName", "zos.lpar");

            apiml = new ServiceInfo.Apiml();
            apiInfo = ServiceInfo.ApiInfoExtended.builder().apiId("zowe.apiml.apicatalog").build();
            apiml.setApiInfo(singletonList(apiInfo));
            ServiceInfo.Instances instance = ServiceInfo.Instances.builder()
                    .customMetadata(Maps.of("zos.sysname", "MD20",
                            "zos.sysplex", "PLEXM2",
                            "zos.jobid", "TST71112"
                    )).build();

            //Well decorated service
            Map<String, ServiceInfo.Instances> instances = Maps.of("dummy:host:14", instance);
            serviceOne = ServiceInfo.builder().serviceId("serviceId")
                    .status(InstanceInfo.InstanceStatus.UP)
                    .apiml(apiml)
                    .instances(instances)
                    .build();
        }

        @Test
        void shouldMapServiceInfoWithFilteredMetadata() {
            ApimlInfo info = centralApimlInfoMapper.buildApimlServiceInfo("apiml1", singletonList(serviceOne));

            assertThat(info.getApimlId()).isEqualTo("apiml1");
            CentralServiceInfo centralService = info.getServices().get(0);

            assertThat(centralService.getServiceId()).isEqualTo("serviceId");
            assertThat(centralService.getStatus()).isEqualTo(InstanceInfo.InstanceStatus.UP);
            assertThat(centralService.getApiId()).containsOnly("zowe.apiml.apicatalog");
            assertThat(centralService.getCustomMetadata()).containsOnlyKeys("zos.sysname", "zos.sysplex");
        }

        @Test
        void shouldMapServiceInfoWithMultipleAPIs() {
            ServiceInfo.ApiInfoExtended apiInfo2 = ServiceInfo.ApiInfoExtended.builder().apiId("zowe.apiml.gateway").build();
            apiml.setApiInfo(Arrays.asList(apiInfo, apiInfo2));

            ApimlInfo info = centralApimlInfoMapper.buildApimlServiceInfo("apiml1", singletonList(serviceOne));

            CentralServiceInfo centralService = info.getServices().get(0);
            assertThat(centralService.getApiId()).containsOnly("zowe.apiml.apicatalog", "zowe.apiml.gateway");
        }

        @Test
        void shouldMapListOfServices() {
            //bare minimal registration
            ServiceInfo serviceTwo = ServiceInfo.builder().serviceId("minimal")
                    .status(InstanceInfo.InstanceStatus.UNKNOWN)
                    .build();

            ApimlInfo info = centralApimlInfoMapper.buildApimlServiceInfo("apiml1", asList(serviceOne, serviceTwo));

            assertThat(info.getApimlId()).isEqualTo("apiml1");
            assertThat(info.getServices()).hasSize(2);

            CentralServiceInfo minimalService = info.getServices().get(1);
            assertThat(minimalService.getServiceId()).isEqualTo("minimal");
            assertThat(minimalService.getApiId()).isEmpty();
            assertThat(minimalService.getCustomMetadata()).isEmpty();
            assertThat(minimalService.getStatus()).isEqualTo(InstanceInfo.InstanceStatus.UNKNOWN);
        }

    }

}
