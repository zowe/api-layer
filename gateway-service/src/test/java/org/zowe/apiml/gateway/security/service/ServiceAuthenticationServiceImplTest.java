/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.zuul.context.RequestContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.config.service.security.MockedSecurityContext;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.gateway.cache.RetryIfExpiredAspect;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.schema.*;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.gateway.utils.CurrentRequestContextTest;
import org.zowe.apiml.util.CacheUtils;

import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    MockedSecurityContext.class,
    CacheConfig.class,
    RetryIfExpiredAspect.class
})
@EnableAspectJAutoProxy
class ServiceAuthenticationServiceImplTest extends CurrentRequestContextTest {

    @Autowired
    private EurekaClient discoveryClient;

    @Autowired
    private AuthenticationSchemeFactory authenticationSchemeFactory;

    @Autowired
    private AuthSourceService authSourceService;

    @Autowired
    private ServiceAuthenticationService serviceAuthenticationService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * secondary instance to check protected methods
     */
    private ServiceAuthenticationServiceImpl serviceAuthenticationServiceImpl;

    @BeforeEach
    void init() {
        lockAndClearRequestContext();
        MockitoAnnotations.initMocks(this);
        RequestContext.testSetCurrentContext(null);
        serviceAuthenticationService.evictCacheAllService();

        serviceAuthenticationServiceImpl = new ServiceAuthenticationServiceImpl(
                discoveryClient,
                new EurekaMetadataParser(),
                authenticationSchemeFactory,
                authSourceService,
                cacheManager,
                new CacheUtils());
    }

    @AfterEach
    void tearDown() {
        unlockRequestContext();
    }

    private InstanceInfo createInstanceInfo(String instanceId, String scheme, String applid) {
        InstanceInfo out = mock(InstanceInfo.class);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(AUTHENTICATION_SCHEME, scheme);
        metadata.put(AUTHENTICATION_APPLID, applid);

        when(out.getMetadata()).thenReturn(metadata);
        when(out.getId()).thenReturn(instanceId);
        when(out.getInstanceId()).thenReturn(instanceId);

        return out;
    }

    private InstanceInfo createInstanceInfo(String instanceId, AuthenticationScheme scheme, String applid) {
        return createInstanceInfo(instanceId, scheme == null ? null : scheme.getScheme(), applid);
    }

    private InstanceInfo createInstanceInfo(String id, Authentication authentication) {
        return createInstanceInfo(id, authentication == null ? null : authentication.getScheme(), authentication == null ? null : authentication.getApplid());
    }

    private Application createApplication(InstanceInfo... instances) {
        final Application out = mock(Application.class);
        when(out.getInstances()).thenReturn(Arrays.asList(instances));
        return out;
    }

    private static Stream<AuthSource> provideAuthSources() {
        return Stream.of(
            new JwtAuthSource("token"),
            new X509AuthSource(mock(X509Certificate.class))
        );
    }

    private static Stream<List<ImmutablePair<AuthSource, AuthSource.Parsed>>> provideAuthSources2Pairs() {
        JwtAuthSource token1 = new JwtAuthSource("token1");
        JwtAuthSource token2 = new JwtAuthSource("token2");
        AuthSource.Parsed jwtParsedSource1 = new JwtAuthSource.Parsed("userId", Date.valueOf(LocalDate.of(1900, 1, 1)), Date.valueOf(LocalDate.of(2100, 1, 1)), Origin.ZOWE);
        AuthSource.Parsed jwtParsedSource2 = new JwtAuthSource.Parsed("userId", Date.valueOf(LocalDate.of(1900, 1, 1)), Date.valueOf(LocalDate.of(2000, 1, 1)), Origin.ZOWE);

        X509AuthSource x509AuthSource1 = new X509AuthSource(mock(X509Certificate.class));
        X509AuthSource x509AuthSource2 = new X509AuthSource(mock(X509Certificate.class));
        AuthSource.Parsed x509ParsedSource1 = new X509AuthSource.Parsed("userId", Date.valueOf(LocalDate.of(1900, 1, 1)), Date.valueOf(LocalDate.of(2100, 1, 1)), Origin.ZOWE, "encoded", "distname");
        AuthSource.Parsed x509ParsedSource2 = new X509AuthSource.Parsed("userId", Date.valueOf(LocalDate.of(1900, 1, 1)), Date.valueOf(LocalDate.of(2000, 1, 1)), Origin.ZOWE, "encoded", "distname");

        List<ImmutablePair<AuthSource, AuthSource.Parsed>> jwtPairs = Arrays.asList(ImmutablePair.of(token1, jwtParsedSource1), ImmutablePair.of(token2, jwtParsedSource2));
        List<ImmutablePair<AuthSource, AuthSource.Parsed>> x509Pairs = Arrays.asList(ImmutablePair.of(x509AuthSource1, x509ParsedSource1), ImmutablePair.of(x509AuthSource2, x509ParsedSource2));
        return Stream.of(jwtPairs, x509Pairs);
    }

    private static Stream<List<AuthSource>> provideAuthSourcesList() {
        JwtAuthSource token1 = new JwtAuthSource("jwt01");
        JwtAuthSource token2 = new JwtAuthSource("jwt02");
        JwtAuthSource token3 = new JwtAuthSource("jwt03");

        X509AuthSource x509AuthSource1 = new X509AuthSource(mock(X509Certificate.class));
        X509AuthSource x509AuthSource2 = new X509AuthSource(mock(X509Certificate.class));
        X509AuthSource x509AuthSource3 = new X509AuthSource(mock(X509Certificate.class));

        List<AuthSource> jwtPairs = Arrays.asList(token1, token2, token3);
        List<AuthSource> x509Pairs = Arrays.asList(x509AuthSource1, x509AuthSource2, x509AuthSource3);
        return Stream.of(jwtPairs, x509Pairs);
    }

    @Test
    void testGetAuthentication() {
        InstanceInfo ii;

        ii = createInstanceInfo("instance1", "bypass", "applid");
        assertEquals(new Authentication(AuthenticationScheme.BYPASS, "applid"), serviceAuthenticationServiceImpl.getAuthentication(ii));

        ii = createInstanceInfo("instance2", "zoweJwt", "applid2");
        assertEquals(new Authentication(AuthenticationScheme.ZOWE_JWT, "applid2"), serviceAuthenticationServiceImpl.getAuthentication(ii));

        ii = createInstanceInfo("instance2", "httpBasicPassTicket", null);
        assertEquals(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, null), serviceAuthenticationServiceImpl.getAuthentication(ii));

        ii = createInstanceInfo("instance2", (AuthenticationScheme) null, null);
        assertEquals(new Authentication(), serviceAuthenticationServiceImpl.getAuthentication(ii));
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources2Pairs")
    void testGetAuthenticationCommand(List<ImmutablePair<AuthSource, AuthSource.Parsed>> authSourcePairs) {
        AuthSource authSource1 = authSourcePairs.get(0).left;
        AuthSource authSource2 = authSourcePairs.get(1).left;
        AuthSource.Parsed parsedAuthSource1 = authSourcePairs.get(0).right;
        AuthSource.Parsed parsedAuthSource2 = authSourcePairs.get(1).right;
        AbstractAuthenticationScheme schemeBeanMock = mock(AbstractAuthenticationScheme.class);

        AuthenticationCommand acValid = spy(new AuthenticationCommandTest(false));
        AuthenticationCommand acExpired = spy(new AuthenticationCommandTest(true));

        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET))
            .thenReturn(schemeBeanMock);
        when(authSourceService.parse(authSource1)).thenReturn(parsedAuthSource1);
        when(authSourceService.parse(authSource2)).thenReturn(parsedAuthSource2);
        when(schemeBeanMock.createCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource1)).thenReturn(acValid);
        when(schemeBeanMock.createCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource2)).thenReturn(acExpired);

        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource1));
        verify(schemeBeanMock, times(1)).createCommand(any(), any());
        // cache is working, it is not expired
        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource1));
        verify(schemeBeanMock, times(1)).createCommand(any(), any());

        // new entry - expired, dont cache that
        assertSame(acExpired, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource2));
        verify(schemeBeanMock, times(2)).createCommand(any(), any());
        // replace result (to know that expired record is removed and get new one)
        when(schemeBeanMock.createCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource2)).thenReturn(acValid);
        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource2));
        verify(schemeBeanMock, times(3)).createCommand(any(), any());
    }

    @Test
    void testGetAuthenticationCommand_whenNoAuthSource() {
        AbstractAuthenticationScheme schemeBeanMock = mock(AbstractAuthenticationScheme.class);
        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET))
            .thenReturn(schemeBeanMock);
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");

        serviceAuthenticationService.getAuthenticationCommand(authentication, null);

        verify(schemeBeanMock, times(1)).createCommand(
            authentication,
            null
        );
    }

    @ParameterizedTest
    @MethodSource("provideAuthSourcesList")
    void testGetAuthenticationCommandByServiceId(List<AuthSource> authSourceTriplet) {
        AuthSource authSource1 = authSourceTriplet.get(0);
        AuthSource authSource2 = authSourceTriplet.get(1);
        AuthSource authSource3 = authSourceTriplet.get(2);
        AuthenticationCommand ok = new AuthenticationCommandTest(false);
        Authentication a1 = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid01");
        Authentication a2 = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid01");
        Authentication a5 = new Authentication(null, null);

        InstanceInfo ii1 = createInstanceInfo("inst01", a1);
        InstanceInfo ii2 = createInstanceInfo("inst01", a2);
        InstanceInfo ii5 = createInstanceInfo("inst02", a5);

        Application application;

        ServiceAuthenticationService sas = spy(serviceAuthenticationServiceImpl);

        AbstractAuthenticationScheme scheme = mock(AbstractAuthenticationScheme.class);
        doAnswer(invocation -> {
            return ok;
        }).when(scheme).createCommand(any(), any());
        when(authenticationSchemeFactory.getSchema(any())).thenReturn(scheme);

        // just one instance
        application = createApplication(ii1);
        when(discoveryClient.getApplication("svr01")).thenReturn(application);
        assertSame(ok, sas.getAuthenticationCommand("svr01", authSource1));

        // multiple same instances
        application = createApplication(ii1, ii1, ii1);
        when(discoveryClient.getApplication("svr02")).thenReturn(application);
        assertSame(ok, sas.getAuthenticationCommand("svr02", authSource2));

        // multiple different instances
        reset(discoveryClient);
        application = createApplication(ii1, ii2);
        when(discoveryClient.getApplication("svr03")).thenReturn(application);
        assertSame(ok, sas.getAuthenticationCommand("svr03", new JwtAuthSource("jwt03")));

        reset(discoveryClient);
        when(discoveryClient.getInstancesById("svr03")).thenReturn(Collections.singletonList(ii5));
        assertSame(AuthenticationCommand.EMPTY, sas.getAuthenticationCommand("svr03", authSource3));

        when(discoveryClient.getInstancesById("svr04")).thenReturn(Collections.emptyList());
        assertSame(AuthenticationCommand.EMPTY, sas.getAuthenticationCommand("svr04", authSource3));
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testGetAuthenticationCommandByServiceIdCache(AuthSource authSource) {
        InstanceInfo ii1 = createInstanceInfo("i1", AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid1");
        AuthenticationCommand ac1 = new AuthenticationCommandTest(true);
        AuthenticationCommand ac2 = new AuthenticationCommandTest(false);
        AbstractAuthenticationScheme aas1 = mock(AbstractAuthenticationScheme.class);
        when(aas1.getScheme()).thenReturn(AuthenticationScheme.HTTP_BASIC_PASSTICKET);
        reset(discoveryClient);

        Application application = createApplication(ii1);
        when(discoveryClient.getApplication("s1")).thenReturn(application);
        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET)).thenReturn(aas1);
        when(aas1.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid1")), any()))
            .thenReturn(ac1);

        assertSame(ac1, serviceAuthenticationService.getAuthenticationCommand("s1", authSource));
        verify(discoveryClient, times(2)).getApplication("s1");

        serviceAuthenticationService.evictCacheAllService();
        Mockito.reset(aas1);
        when(aas1.getScheme()).thenReturn(AuthenticationScheme.HTTP_BASIC_PASSTICKET);
        when(aas1.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid1")), any()))
            .thenReturn(ac2);
        assertSame(ac2, serviceAuthenticationService.getAuthenticationCommand("s1", authSource));
        verify(discoveryClient, times(3)).getApplication("s1");
        assertSame(ac2, serviceAuthenticationService.getAuthenticationCommand("s1", authSource));
        verify(discoveryClient, times(3)).getApplication("s1");
    }

    @ParameterizedTest
    @MethodSource("provideAuthSourcesList")
    void testEvictCacheService(List<AuthSource> authSourceList) {
        AuthSource authSource1 = authSourceList.get(0);
        AuthSource authSource2 = authSourceList.get(1);
        AuthenticationCommand command = AuthenticationCommand.EMPTY;
        reset(discoveryClient);

        Authentication auth = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applicationId0001");
        doReturn(Collections.singletonList(createInstanceInfo("instance0001", auth))).when(discoveryClient).getInstancesById("service0001");
        doReturn(Collections.singletonList(createInstanceInfo("instance0002", auth))).when(discoveryClient).getInstancesById("service0002");
        doReturn(new ByPassScheme()).when(authenticationSchemeFactory).getSchema(auth.getScheme());

        verify(discoveryClient, never()).getInstancesById("service0001");
        verify(discoveryClient, never()).getInstancesById("service0002");

        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", authSource1));
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", authSource1));
        verify(discoveryClient, times(1)).getApplication("service0001");
        verify(discoveryClient, never()).getApplication("service0002");

        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", authSource2));
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", authSource1));
        verify(discoveryClient, times(2)).getApplication("service0001");
        verify(discoveryClient, times(1)).getApplication("service0002");

        serviceAuthenticationService.evictCacheService("service0001");

        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", authSource1));
        verify(discoveryClient, times(3)).getApplication("service0001");
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", authSource2));
        verify(discoveryClient, times(4)).getApplication("service0001");
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", authSource1));
        verify(discoveryClient, times(1)).getApplication("service0002");

        serviceAuthenticationService.evictCacheAllService();
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", authSource1));
        verify(discoveryClient, times(5)).getApplication("service0001");
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", authSource2));
        verify(discoveryClient, times(6)).getApplication("service0001");
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", authSource1));
        verify(discoveryClient, times(2)).getApplication("service0002");
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testNoApplication(AuthSource authSource) {
        when(discoveryClient.getApplication(any())).thenReturn(null);
        assertSame(AuthenticationCommand.EMPTY, serviceAuthenticationServiceImpl.getAuthenticationCommand("unknown", authSource));
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void givenServiceIdAndAuthSource_whenExpiringCommand_thenReturnNewOne(AuthSource authSource) {
        AbstractAuthenticationScheme scheme = mock(AbstractAuthenticationScheme.class);
        Application application = createApplication(
            createInstanceInfo("instanceId", AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid")
        );
        doReturn(application).when(discoveryClient).getApplication("serviceId");
        doReturn(scheme).when(authenticationSchemeFactory).getSchema(any());
        AuthenticationCommandTest cmd = new AuthenticationCommandTest(false);
        doReturn(cmd).when(scheme).createCommand(any(), any());

        // first time, create and put into cache
        assertSame(cmd, serviceAuthenticationService.getAuthenticationCommand("serviceId", authSource));
        verify(scheme, times(1)).createCommand(any(), any());

        // second time, get from cache
        assertSame(cmd, serviceAuthenticationService.getAuthenticationCommand("serviceId", authSource));
        verify(scheme, times(1)).createCommand(any(), any());

        // command expired, take new one
        cmd.setExpired(true);
        AuthenticationCommand cmd2 = new AuthenticationCommandTest(false);
        reset(scheme);
        doReturn(cmd2).when(scheme).createCommand(any(), any());
        assertSame(cmd2, serviceAuthenticationService.getAuthenticationCommand("serviceId", authSource));
        verify(scheme, times(1)).createCommand(any(), any());

        // second command is cached now
        assertSame(cmd2, serviceAuthenticationService.getAuthenticationCommand("serviceId", authSource));
        verify(scheme, times(1)).createCommand(any(), any());
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public class AuthenticationCommandTest extends AuthenticationCommand {

        private static final long serialVersionUID = 8527412076986152763L;

        private boolean expired;

        @Override
        public void apply(InstanceInfo instanceInfo) {
        }

        @Override
        public boolean isRequiredValidSource() {
            return false;
        }

    }

}
