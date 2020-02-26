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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.schema.*;
import org.zowe.apiml.gateway.utils.CurrentRequestContextTest;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.zuul.context.RequestContext;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;
import static org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl.AUTHENTICATION_COMMAND_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    ServiceAuthenticationServiceImplTest.Context.class,
    CacheConfig.class
})
public class ServiceAuthenticationServiceImplTest extends CurrentRequestContextTest {

    @Autowired
    private EurekaClient discoveryClient;

    @Autowired
    private AuthenticationSchemeFactory authenticationSchemeFactory;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ServiceAuthenticationService serviceAuthenticationService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * secondary instance to check protected methods
     */
    private ServiceAuthenticationServiceImpl serviceAuthenticationServiceImpl;

    @BeforeEach
    public void init() {
        lockAndClearRequestContext();
        MockitoAnnotations.initMocks(this);
        RequestContext.testSetCurrentContext(null);
        serviceAuthenticationService.evictCacheAllService();

        serviceAuthenticationServiceImpl = new ServiceAuthenticationServiceImpl(discoveryClient, authenticationSchemeFactory, authenticationService, cacheManager);
    }

    @AfterEach
    public void tearDown() {
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

    @Test
    public void testGetAuthentication() {
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

    @Test
    public void testGetAuthenticationCommand() throws Exception {
        AbstractAuthenticationScheme schemeBeanMock = mock(AbstractAuthenticationScheme.class);
        // token1 - valid
        QueryResponse qr1 = new QueryResponse("domain", "userId",
            Date.valueOf(LocalDate.of(1900, 1, 1)),
            Date.valueOf(LocalDate.of(2100, 1, 1))
        );
        // token2 - expired
        QueryResponse qr2 = new QueryResponse("domain", "userId",
            Date.valueOf(LocalDate.of(1900, 1, 1)),
            Date.valueOf(LocalDate.of(2000, 1, 1))
        );
        AuthenticationCommand acValid = spy(new AuthenticationCommandTest(false));
        AuthenticationCommand acExpired = spy(new AuthenticationCommandTest(true));

        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET))
            .thenReturn(schemeBeanMock);
        when(authenticationService.parseJwtToken("token1")).thenReturn(qr1);
        when(authenticationService.parseJwtToken("token2")).thenReturn(qr2);
        when(schemeBeanMock.createCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), qr1)).thenReturn(acValid);
        when(schemeBeanMock.createCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), qr2)).thenReturn(acExpired);

        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), "token1"));
        verify(schemeBeanMock, times(1)).createCommand(any(), any());
        // cache is working, it is not expired
        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), "token1"));
        verify(schemeBeanMock, times(1)).createCommand(any(), any());

        // new entry - expired, dont cache that
        assertSame(acExpired, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), "token2"));
        verify(schemeBeanMock, times(2)).createCommand(any(), any());
        // replace result (to know that expired record is removed and get new one)
        when(schemeBeanMock.createCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), qr2)).thenReturn(acValid);
        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), "token2"));
        verify(schemeBeanMock, times(3)).createCommand(any(), any());
    }

    @Test
    public void testGetAuthenticationCommandByServiceId() throws Exception {
        AuthenticationCommand ok = new AuthenticationCommandTest(false);
        Authentication a1 = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid01");
        Authentication a2 = new Authentication(AuthenticationScheme.ZOWE_JWT, null);
        Authentication a3 = new Authentication(AuthenticationScheme.ZOWE_JWT, "applid01");
        Authentication a4 = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid02");
        Authentication a5 = new Authentication(null, null);

        InstanceInfo ii1 = createInstanceInfo("inst01", a1);
        InstanceInfo ii2 = createInstanceInfo("inst01", a2);
        InstanceInfo ii3 = createInstanceInfo("inst01", a3);
        InstanceInfo ii4 = createInstanceInfo("inst01", a4);
        InstanceInfo ii5 = createInstanceInfo("inst02", a5);

        Application application;

        ServiceAuthenticationService sas = spy(serviceAuthenticationServiceImpl);

        when(authenticationSchemeFactory.getSchema(any())).thenReturn(mock(AbstractAuthenticationScheme.class));

        // just one instance
        when(sas.getAuthenticationCommand(a1, "jwt01")).thenReturn(ok);
        application = createApplication(ii1);
        when(discoveryClient.getApplication("svr01")).thenReturn(application);
        assertSame(ok, sas.getAuthenticationCommand("svr01", "jwt01"));

        // multiple same instances
        when(sas.getAuthenticationCommand(a1, "jwt02")).thenReturn(ok);
        application = createApplication(ii1, ii1, ii1);
        when(discoveryClient.getApplication("svr02")).thenReturn(application);
        assertSame(ok, sas.getAuthenticationCommand("svr02", "jwt02"));

        // multiple different instances
        reset(discoveryClient);
        application = createApplication(ii1, ii2);
        when(discoveryClient.getApplication("svr03")).thenReturn(application);
        assertTrue(sas.getAuthenticationCommand("svr03", "jwt03") instanceof ServiceAuthenticationServiceImpl.LoadBalancerAuthenticationCommand);

        reset(discoveryClient);
        application = createApplication(ii1, ii3);
        when(discoveryClient.getApplication("svr03")).thenReturn(application);
        assertTrue(sas.getAuthenticationCommand("svr03", "jwt03") instanceof ServiceAuthenticationServiceImpl.LoadBalancerAuthenticationCommand);

        reset(discoveryClient);
        application = createApplication(ii1, ii4);
        when(discoveryClient.getApplication("svr03")).thenReturn(application);
        assertTrue(sas.getAuthenticationCommand("svr03", "jwt03") instanceof ServiceAuthenticationServiceImpl.LoadBalancerAuthenticationCommand);

        reset(discoveryClient);
        application = createApplication(ii1, ii2, ii3, ii4);
        when(discoveryClient.getApplication("svr03")).thenReturn(application);
        assertTrue(sas.getAuthenticationCommand("svr03", "jwt03") instanceof ServiceAuthenticationServiceImpl.LoadBalancerAuthenticationCommand);

        reset(discoveryClient);
        when(discoveryClient.getInstancesById("svr03")).thenReturn(Collections.singletonList(ii5));
        assertSame(AuthenticationCommand.EMPTY, sas.getAuthenticationCommand("svr03", "jwt03"));

        when(discoveryClient.getInstancesById("svr04")).thenReturn(Collections.emptyList());
        assertSame(AuthenticationCommand.EMPTY, sas.getAuthenticationCommand("svr04", "jwt03"));
    }

    @Test
    public void testGetAuthenticationCommandByServiceIdCache() throws Exception {
        InstanceInfo ii1 = createInstanceInfo("i1", AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid1");
        AuthenticationCommand ac1 = new AuthenticationCommandTest(true);
        AuthenticationCommand ac2 = new AuthenticationCommandTest(false);
        AbstractAuthenticationScheme aas1 = mock(AbstractAuthenticationScheme.class);
        when(aas1.getScheme()).thenReturn(AuthenticationScheme.HTTP_BASIC_PASSTICKET);

        Application application = createApplication(ii1);
        when(discoveryClient.getApplication("s1")).thenReturn(application);
        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET)).thenReturn(aas1);
        when(aas1.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid1")), any()))
            .thenReturn(ac1);

        assertSame(ac1, serviceAuthenticationService.getAuthenticationCommand("s1", "jwt"));
        verify(discoveryClient, times(1)).getApplication("s1");

        serviceAuthenticationService.evictCacheAllService();
        Mockito.reset(aas1);
        when(aas1.getScheme()).thenReturn(AuthenticationScheme.HTTP_BASIC_PASSTICKET);
        when(aas1.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid1")), any()))
            .thenReturn(ac2);
        assertSame(ac2, serviceAuthenticationService.getAuthenticationCommand("s1", "jwt"));
        verify(discoveryClient, times(2)).getApplication("s1");
        assertSame(ac2, serviceAuthenticationService.getAuthenticationCommand("s1", "jwt"));
        verify(discoveryClient, times(2)).getApplication("s1");
    }

    @Test
    public void testUniversalAuthenticationCommand() throws Exception {
        ServiceAuthenticationServiceImpl.UniversalAuthenticationCommand uac = serviceAuthenticationServiceImpl.new UniversalAuthenticationCommand();
        assertFalse(uac.isExpired());

        try {
            uac.apply(null);
            fail();
        } catch (NullPointerException e) {
            // this command cannot be applied without parameter (null)
        }

        AuthenticationCommand ac = mock(AuthenticationCommand.class);
        InstanceInfo ii = createInstanceInfo("inst0001", AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid0001");
        RequestContext requestContext = mock(RequestContext.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(requestContext.getRequest()).thenReturn(request);
        RequestContext.testSetCurrentContext(requestContext);
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("jwtToken01"));
        AbstractAuthenticationScheme scheme = mock(AbstractAuthenticationScheme.class);
        when(scheme.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid0001")), any())).thenReturn(ac);
        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET)).thenReturn(scheme);

        uac.apply(ii);

        verify(ac, times(1)).apply(null);
    }

    @Test
    public void testLoadBalancerAuthenticationCommand() {
        ServiceAuthenticationServiceImpl.LoadBalancerAuthenticationCommand lbac = serviceAuthenticationServiceImpl.new LoadBalancerAuthenticationCommand();
        assertFalse(lbac.isExpired());

        RequestContext requestContext = new RequestContext();
        RequestContext.testSetCurrentContext(requestContext);

        assertNull(requestContext.get(AUTHENTICATION_COMMAND_KEY));
        lbac.apply(null);
        assertTrue(requestContext.get(AUTHENTICATION_COMMAND_KEY) instanceof ServiceAuthenticationServiceImpl.UniversalAuthenticationCommand);
    }

    @Test
    public void testEvictCacheService() throws Exception {
        AuthenticationCommand command = AuthenticationCommand.EMPTY;
        Authentication auth = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applicationId0001");
        doReturn(Collections.singletonList(createInstanceInfo("instance0001", auth))).when(discoveryClient).getInstancesById("service0001");
        doReturn(Collections.singletonList(createInstanceInfo("instance0002", auth))).when(discoveryClient).getInstancesById("service0002");
        doReturn(new ByPassScheme()).when(authenticationSchemeFactory).getSchema(auth.getScheme());

        verify(discoveryClient, never()).getInstancesById("service0001");
        verify(discoveryClient, never()).getInstancesById("service0002");

        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", "jwt01"));
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", "jwt01"));
        verify(discoveryClient, times(1)).getApplication("service0001");
        verify(discoveryClient, never()).getApplication("service0002");

        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", "jwt02"));
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", "jwt01"));
        verify(discoveryClient, times(2)).getApplication("service0001");
        verify(discoveryClient, times(1)).getApplication("service0002");

        serviceAuthenticationService.evictCacheService("service0001");

        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", "jwt01"));
        verify(discoveryClient, times(3)).getApplication("service0001");
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", "jwt02"));
        verify(discoveryClient, times(4)).getApplication("service0001");
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", "jwt01"));
        verify(discoveryClient, times(1)).getApplication("service0002");

        serviceAuthenticationService.evictCacheAllService();
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", "jwt01"));
        verify(discoveryClient, times(5)).getApplication("service0001");
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0001", "jwt02"));
        verify(discoveryClient, times(6)).getApplication("service0001");
        assertSame(command, serviceAuthenticationService.getAuthenticationCommand("service0002", "jwt01"));
        verify(discoveryClient, times(2)).getApplication("service0002");
    }

    @Test
    public void testNoApplication() throws Exception {
        when(discoveryClient.getApplication(any())).thenReturn(null);
        assertSame(AuthenticationCommand.EMPTY, serviceAuthenticationServiceImpl.getAuthenticationCommand("unknown", "jwtToken"));
    }

    public class AuthenticationCommandTest extends AuthenticationCommand {

        private static final long serialVersionUID = 8527412076986152763L;

        private boolean expired;

        public AuthenticationCommandTest(boolean expired) {
            this.expired = expired;
        }

        @Override
        public boolean isExpired() {
            return expired;
        }

        @Override
        public void apply(InstanceInfo instanceInfo) {
        }

    }

    @Configuration
    public static class Context {

        @Bean
        public EurekaClient getDiscoveryClient() {
            return mock(EurekaClient.class);
        }

        @Bean
        public AuthenticationSchemeFactory getAuthenticationSchemeFactory() {
            return mock(AuthenticationSchemeFactory.class);
        }

        @Bean
        public AuthenticationService getAuthenticationService() {
            return mock(AuthenticationService.class);
        }

        @Bean
        public ServiceAuthenticationService getServiceAuthenticationService(@Autowired CacheManager cacheManager) {
            return new ServiceAuthenticationServiceImpl(getDiscoveryClient(), getAuthenticationSchemeFactory(), getAuthenticationService(), cacheManager);
        }

    }

}
