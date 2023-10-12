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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
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
import org.zowe.apiml.gateway.security.service.schema.source.*;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.gateway.utils.CurrentRequestContextTest;
import org.zowe.apiml.util.CacheUtils;

import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;
import static org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl.CACHE_BY_AUTHENTICATION;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    MockedSecurityContext.class,
    CacheConfig.class,
    RetryIfExpiredAspect.class
})
@EnableAspectJAutoProxy
class ServiceAuthenticationServiceImplTest extends CurrentRequestContextTest {

    @Autowired
    private ApplicationContext applicationContext;

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
        MockitoAnnotations.openMocks(this);
        RequestContext.testSetCurrentContext(null);
        serviceAuthenticationService.evictCacheAllService();

        serviceAuthenticationServiceImpl = new ServiceAuthenticationServiceImpl(
            applicationContext,
            discoveryClient,
            new EurekaMetadataParser(),
            authenticationSchemeFactory,
            cacheManager,
            new CacheUtils());
        serviceAuthenticationServiceImpl.afterPropertiesSet();
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
        AuthSource.Parsed jwtParsedSource1 = new ParsedTokenAuthSource("userId", Date.valueOf(LocalDate.of(1900, 1, 1)), Date.valueOf(LocalDate.of(2100, 1, 1)), Origin.ZOWE);
        AuthSource.Parsed jwtParsedSource2 = new ParsedTokenAuthSource("userId", Date.valueOf(LocalDate.of(1900, 1, 1)), Date.valueOf(LocalDate.of(2000, 1, 1)), Origin.ZOWE);

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
    void testGetAuthenticationFromInstanceInfo() {
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

    @Nested
    class GivenServiceId {
        Application application;
        ServiceAuthenticationService sas;

        @BeforeEach
        void setup() {
            sas = spy(serviceAuthenticationServiceImpl);
        }

        @Nested
        class WhenNoApplicationsDiscovered {
            @Test
            void thenReturnNull() {
                when(discoveryClient.getApplication("svr01")).thenReturn(null);
                assertNull(sas.getAuthentication("svr01"));
            }
        }

        @Nested
        class WhenNoInstancesDiscovered {
            @Test
            void thenReturnNull() {
                application = createApplication();
                when(discoveryClient.getApplication("svr01")).thenReturn(null);
                assertNull(sas.getAuthentication("svr01"));
            }
        }

        @Nested
        class WhenApplicationDiscovered {
            Authentication a1 = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid01");
            Authentication a4 = new Authentication(null, null);

            InstanceInfo ii1 = createInstanceInfo("inst01", a1);
            InstanceInfo ii4 = createInstanceInfo("inst02", a4);
            InstanceInfo ii5 = createInstanceInfo("inst02", null);


            @Nested
            class AndHaveMultipleInstances {
                @Test
                void andAllInstancesHasSameAuthentication_thenReturnAuthentication() {
                    application = createApplication(ii1, ii1, ii1);
                    when(discoveryClient.getApplication("svr01")).thenReturn(application);
                    assertEquals(a1, sas.getAuthentication("svr01"));
                }

                @Test
                void andInstancesHaveEmptyAuthentication_thenReturnNull() {
                    application = createApplication(ii4, ii4);
                    when(discoveryClient.getApplication("svr01")).thenReturn(application);
                    assertEquals(a4, sas.getAuthentication("svr01"));
                }

                @Test
                void andInstancesHaveNullAuthentication_thenReturnNull() {
                    application = createApplication(ii5, ii5);
                    when(discoveryClient.getApplication("svr01")).thenReturn(application);
                    assertEquals(a4, sas.getAuthentication("svr01"));
                }
            }

            @Nested
            class AndHaveOneInstance {
                @Test
                void foundAuthenticationIsNull_thenReturnNull() {
                    application = createApplication(ii5);
                    when(discoveryClient.getApplication("svr01")).thenReturn(application);
                    assertEquals(a4, sas.getAuthentication("svr01"));
                }

                @Test
                void foundAuthenticationIsEmpty_thenReturnNull() {
                    application = createApplication(ii4);
                    when(discoveryClient.getApplication("svr01")).thenReturn(application);
                    assertEquals(a4, sas.getAuthentication("svr01"));
                }
            }
        }

    }

    @ParameterizedTest
    @MethodSource("provideAuthSources2Pairs")
    void testGetAuthenticationCommand(List<ImmutablePair<AuthSource, AuthSource.Parsed>> authSourcePairs) {
        AuthSource authSource1 = authSourcePairs.get(0).left;
        AuthSource authSource2 = authSourcePairs.get(1).left;
        AuthSource.Parsed parsedAuthSource1 = authSourcePairs.get(0).right;
        AuthSource.Parsed parsedAuthSource2 = authSourcePairs.get(1).right;
        IAuthenticationScheme schemeBeanMock = mock(IAuthenticationScheme.class);

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

        // new entry - expired, dont cache that (+ retry aspect)
        assertSame(acExpired, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource2));
        verify(schemeBeanMock, times(3)).createCommand(any(), any());
        // replace result (to know that expired record is removed and get new one)
        when(schemeBeanMock.createCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource2)).thenReturn(acValid);
        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), authSource2));
        verify(schemeBeanMock, times(4)).createCommand(any(), any());
    }

    @Test
    void testGetAuthenticationCommand_whenNoAuthSource() {
        IAuthenticationScheme schemeBeanMock = mock(IAuthenticationScheme.class);
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
        AuthSource authSource = authSourceTriplet.get(0);
        AuthenticationCommand ok = new AuthenticationCommandTest(false);
        Authentication a1 = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid01");
        Authentication ea = new Authentication(null, null);


        ServiceAuthenticationService sas = spy(serviceAuthenticationServiceImpl);

        IAuthenticationScheme scheme = mock(IAuthenticationScheme.class);
        doAnswer(invocation -> ok).when(scheme).createCommand(any(), any());
        when(authenticationSchemeFactory.getSchema(any())).thenReturn(scheme);

        // normal authentication as parameter (just one instance or multiple instances with same authentication)
        assertSame(ok, sas.getAuthenticationCommand("svr01", a1, authSource));

        // loadBalanceAuthentication as parameter (multiple different instances)

        // empty authentication
        assertSame(AuthenticationCommand.EMPTY, sas.getAuthenticationCommand("svr03", ea, authSource));

        // null authentication
        assertSame(AuthenticationCommand.EMPTY, sas.getAuthenticationCommand("svr04", null, authSource));
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testGetAuthenticationCommandByServiceIdCache(AuthSource authSource) {
        AuthenticationCommand ac1 = new AuthenticationCommandTest(true);
        AuthenticationCommand ac2 = new AuthenticationCommandTest(false);
        IAuthenticationScheme schemeBeanMock = mock(IAuthenticationScheme.class);
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid1");

        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET)).thenReturn(schemeBeanMock);
        when(schemeBeanMock.createCommand(eq(authentication), any())).thenReturn(ac1);

        assertSame(ac1, serviceAuthenticationService.getAuthenticationCommand("s1", authentication, authSource));
        // two cached method, if response is expired then retry - there for 4 instances - just for test, normally it is not generated as expired
        verify(schemeBeanMock, times(4)).createCommand(authentication, authSource);

        serviceAuthenticationService.evictCacheAllService();
        Mockito.reset(schemeBeanMock);
        when(schemeBeanMock.getScheme()).thenReturn(AuthenticationScheme.HTTP_BASIC_PASSTICKET);

        when(schemeBeanMock.createCommand(eq(authentication), any())).thenReturn(ac2);

        assertSame(ac2, serviceAuthenticationService.getAuthenticationCommand("s1", authentication, authSource));
        verify(schemeBeanMock, times(1)).createCommand(authentication, authSource);

        assertSame(ac2, serviceAuthenticationService.getAuthenticationCommand("s1", authentication, authSource));
        verify(schemeBeanMock, times(1)).createCommand(authentication, authSource);
    }

    @ParameterizedTest
    @MethodSource("provideAuthSourcesList")
    void testEvictCacheService(List<AuthSource> authSourceList) {
        AuthSource authSource1 = authSourceList.get(0);
        AuthSource authSource2 = authSourceList.get(1);
        AuthenticationCommand command = AuthenticationCommand.EMPTY;
        IAuthenticationScheme baypassSchemeMock = mock(ByPassScheme.class);

        Authentication auth1 = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applicationId0001");
        Authentication auth2 = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applicationId0002");
        doReturn(baypassSchemeMock).when(authenticationSchemeFactory).getSchema(auth1.getScheme());
        doReturn(baypassSchemeMock).when(authenticationSchemeFactory).getSchema(auth2.getScheme());
        when(baypassSchemeMock.createCommand(eq(auth1), any())).thenReturn(command);
        when(baypassSchemeMock.createCommand(eq(auth2), any())).thenReturn(command);

        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth1, authSource1));
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth1, authSource1));
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth2, authSource1));
        verify(baypassSchemeMock, times(1)).createCommand(auth1, authSource1);
        verify(baypassSchemeMock, times(1)).createCommand(auth2, authSource1);

        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth1, authSource2));
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", auth1, authSource1));
        verify(baypassSchemeMock, times(1)).createCommand(auth1, authSource1);
        verify(baypassSchemeMock, times(1)).createCommand(auth1, authSource2);

        serviceAuthenticationService.evictCacheService("service0001");
        cacheManager.getCache(CACHE_BY_AUTHENTICATION).clear();
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth1, authSource1));
        verify(baypassSchemeMock, times(2)).createCommand(auth1, authSource1);
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth1, authSource2));
        verify(baypassSchemeMock, times(2)).createCommand(auth1, authSource2);
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth2, authSource1));
        verify(baypassSchemeMock, times(2)).createCommand(auth2, authSource1);
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", auth1, authSource1));
        verify(baypassSchemeMock, times(2)).createCommand(auth1, authSource1);

        serviceAuthenticationService.evictCacheAllService();
        cacheManager.getCache(CACHE_BY_AUTHENTICATION).clear();
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth1, authSource1));
        verify(baypassSchemeMock, times(3)).createCommand(auth1, authSource1);
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth1, authSource2));
        verify(baypassSchemeMock, times(3)).createCommand(auth1, authSource2);
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", auth2, authSource2));
        verify(baypassSchemeMock, times(1)).createCommand(auth2, authSource2);
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", auth1, authSource1));
        verify(baypassSchemeMock, times(3)).createCommand(auth1, authSource1);
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", auth2, authSource1));
        verify(baypassSchemeMock, times(3)).createCommand(auth2, authSource1);
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void testNoApplication(AuthSource authSource) {
        when(discoveryClient.getApplication(any())).thenReturn(null);
        assertSame(AuthenticationCommand.EMPTY, serviceAuthenticationServiceImpl.getAuthenticationCommand("unknown", null, authSource));
    }

    @ParameterizedTest
    @MethodSource("provideAuthSources")
    void givenServiceIdAndAuthSource_whenExpiringCommand_thenReturnNewOne(AuthSource authSource) {
        IAuthenticationScheme scheme = mock(IAuthenticationScheme.class);
        Application application = createApplication(
            createInstanceInfo("instanceId", AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid")
        );
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        doReturn(application).when(discoveryClient).getApplication("serviceId");
        doReturn(scheme).when(authenticationSchemeFactory).getSchema(any());
        AuthenticationCommandTest cmd = new AuthenticationCommandTest(false);
        doReturn(cmd).when(scheme).createCommand(any(), any());

        // first time, create and put into cache
        assertSame(cmd, serviceAuthenticationService.getAuthenticationCommand("serviceId", authentication, authSource));
        verify(scheme, times(1)).createCommand(any(), any());

        // second time, get from cache
        assertSame(cmd, serviceAuthenticationService.getAuthenticationCommand("serviceId", authentication, authSource));
        verify(scheme, times(1)).createCommand(any(), any());

        // command expired, take new one
        cmd.setExpired(true);
        AuthenticationCommand cmd2 = new AuthenticationCommandTest(false);
        reset(scheme);
        doReturn(cmd2).when(scheme).createCommand(any(), any());
        assertSame(cmd2, serviceAuthenticationService.getAuthenticationCommand("serviceId", authentication, authSource));
        verify(scheme, times(1)).createCommand(any(), any());

        // second command is cached now
        assertSame(cmd2, serviceAuthenticationService.getAuthenticationCommand("serviceId", authentication, authSource));
        verify(scheme, times(1)).createCommand(any(), any());
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class AuthenticationCommandTest extends AuthenticationCommand {

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
