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
import com.netflix.loadbalancer.reactive.ExecutionListener;
import com.netflix.zuul.context.RequestContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.config.service.security.MockedSecurityContext;
import org.zowe.apiml.gateway.cache.RetryIfExpiredAspect;
import org.zowe.apiml.gateway.config.CacheConfig;
import org.zowe.apiml.gateway.security.service.schema.*;
import org.zowe.apiml.gateway.utils.CurrentRequestContextTest;
import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.CacheUtils;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_APPLID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.AUTHENTICATION_SCHEME;
import static org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl.AUTHENTICATION_COMMAND_KEY;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    MockedSecurityContext.class,
    CacheConfig.class,
    RetryIfExpiredAspect.class
})
@EnableAspectJAutoProxy
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

        serviceAuthenticationServiceImpl = new ServiceAuthenticationServiceImpl(discoveryClient, authenticationSchemeFactory, authenticationService, cacheManager, new CacheUtils());
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
    public void testGetAuthenticationCommand() {
        AbstractAuthenticationScheme schemeBeanMock = mock(AbstractAuthenticationScheme.class);
        // token1 - valid
        QueryResponse qr1 = new QueryResponse("domain", "userId",
            Date.valueOf(LocalDate.of(1900, 1, 1)),
            Date.valueOf(LocalDate.of(2100, 1, 1)),
            QueryResponse.Source.ZOWE
        );
        // token2 - expired
        QueryResponse qr2 = new QueryResponse("domain", "userId",
            Date.valueOf(LocalDate.of(1900, 1, 1)),
            Date.valueOf(LocalDate.of(2000, 1, 1)),
            QueryResponse.Source.ZOWE
        );
        AuthenticationCommand acValid = spy(new AuthenticationCommandTest(false));
        AuthenticationCommand acExpired = spy(new AuthenticationCommandTest(true));

        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET))
            .thenReturn(schemeBeanMock);
        when(authenticationService.parseJwtToken("token1")).thenReturn(qr1);
        when(authenticationService.parseJwtToken("token2")).thenReturn(qr2);
        when(schemeBeanMock.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid")), argThat(x -> Objects.equals(x.get(), qr1)))).thenReturn(acValid);
        when(schemeBeanMock.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid")), argThat(x -> Objects.equals(x.get(), qr2)))).thenReturn(acExpired);

        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), "token1"));
        verify(schemeBeanMock, times(1)).createCommand(any(), any());
        // cache is working, it is not expired
        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), "token1"));
        verify(schemeBeanMock, times(1)).createCommand(any(), any());

        // new entry - expired, dont cache that
        assertSame(acExpired, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), "token2"));
        verify(schemeBeanMock, times(2)).createCommand(any(), any());
        // replace result (to know that expired record is removed and get new one)
        when(schemeBeanMock.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid")), argThat(x -> Objects.equals(x.get(), qr2)))).thenReturn(acValid);
        assertSame(acValid, serviceAuthenticationService.getAuthenticationCommand(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid"), "token2"));
        verify(schemeBeanMock, times(3)).createCommand(any(), any());
    }

    @Test
    public void testGetAuthenticationCommand_whenNoJwt() {
        AbstractAuthenticationScheme schemeBeanMock = mock(AbstractAuthenticationScheme.class);
        when(authenticationSchemeFactory.getSchema(AuthenticationScheme.HTTP_BASIC_PASSTICKET))
            .thenReturn(schemeBeanMock);
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");

        serviceAuthenticationService.getAuthenticationCommand(authentication, null);

        verify(schemeBeanMock, times(1)).createCommand(
            eq(authentication),
            argThat(x -> x.get() == null)
        );
    }

    @Test
    public void testGetAuthenticationCommandByServiceId() {
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

        //AbstractAuthenticationScheme scheme = mock(AbstractAuthenticationScheme.class);
        AbstractAuthenticationScheme scheme = mock(AbstractAuthenticationScheme.class);
        doAnswer(invocation -> {
            ((Supplier<?>) invocation.getArgument(1)).get();
            return ok;
        }).when(scheme).createCommand(any(), any());
        when(authenticationSchemeFactory.getSchema(any())).thenReturn(scheme);

        // just one instance
        application = createApplication(ii1);
        when(discoveryClient.getApplication("svr01")).thenReturn(application);
        assertSame(ok, sas.getAuthenticationCommand("svr01", "jwt01"));

        // multiple same instances
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
    public void testGetAuthenticationCommandByServiceIdCache() {
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
        verify(discoveryClient, times(2)).getApplication("s1");

        serviceAuthenticationService.evictCacheAllService();
        Mockito.reset(aas1);
        when(aas1.getScheme()).thenReturn(AuthenticationScheme.HTTP_BASIC_PASSTICKET);
        when(aas1.createCommand(eq(new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid1")), any()))
            .thenReturn(ac2);
        assertSame(ac2, serviceAuthenticationService.getAuthenticationCommand("s1", "jwt"));
        verify(discoveryClient, times(3)).getApplication("s1");
        assertSame(ac2, serviceAuthenticationService.getAuthenticationCommand("s1", "jwt"));
        verify(discoveryClient, times(3)).getApplication("s1");
    }

    @Test
    public void testUniversalAuthenticationCommand() {
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
        assertFalse(lbac.isRequiredValidJwt());
    }

    @Test
    public void testEvictCacheService() {
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
    public void testNoApplication() {
        when(discoveryClient.getApplication(any())).thenReturn(null);
        assertSame(AuthenticationCommand.EMPTY, serviceAuthenticationServiceImpl.getAuthenticationCommand("unknown", "jwtToken"));
    }

    @Test
    public void testIsRequiredValidJwt() {
        ServiceAuthenticationServiceImpl.UniversalAuthenticationCommand universalAuthenticationCommand = serviceAuthenticationServiceImpl.new UniversalAuthenticationCommand();
        assertFalse(universalAuthenticationCommand.isRequiredValidJwt());
    }

    private <T> T getUnProxy(T springClass) throws Exception {
        if (springClass instanceof  Advised) {
            return (T) ((Advised) springClass).getTargetSource().getTarget();
        }
        return springClass;
    }

    private AuthenticationCommand testRequiredAuthentication(boolean requiredJwtValidation, String jwtToken) throws Exception {
        Authentication authentication = new Authentication(AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid");
        ServiceAuthenticationServiceImpl.UniversalAuthenticationCommand universalAuthenticationCommand =
            serviceAuthenticationServiceImpl.new UniversalAuthenticationCommand();

        AuthenticationCommand ac = mock(AuthenticationCommand.class);
        QueryResponse queryResponse = mock(QueryResponse.class);
        AbstractAuthenticationScheme schema = mock(AbstractAuthenticationScheme.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestContext.getCurrentContext().setRequest(request);

        Stubber stubber;
        if (StringUtils.equals(jwtToken, "validJwt")) {
            stubber = doReturn(Optional.of(jwtToken));
        } else {
            stubber = doThrow(new TokenNotValidException("Token is not valid."));
        }
        stubber.when(getUnProxy(authenticationService)).getJwtTokenFromRequest(request);
        doReturn(ac).when(schema).createCommand(eq(authentication), argThat(x -> Objects.equals(x.get(), queryResponse)));
        doReturn(schema).when(getUnProxy(authenticationSchemeFactory)).getSchema(authentication.getScheme());
        doReturn(queryResponse).when(getUnProxy(authenticationService)).parseJwtToken("validJwt");
        doReturn(requiredJwtValidation).when(ac).isRequiredValidJwt();

        universalAuthenticationCommand.apply(createInstanceInfo("id", authentication));

        return ac;
    }

    @Test
    public void givenMissingJwt_whenCommandRequiredAuthentication_thenReject() throws Exception {
        try {
            testRequiredAuthentication(true, null);
            fail();
        } catch (ExecutionListener.AbortExecutionException aee) {
            assertTrue(aee.getMessage().contains("Invalid JWT token"));
        }
    }

    @Test
    public void givenInvalidJwt_whenCommandRequiredAuthentication_thenReject() throws Exception {
        try {
            testRequiredAuthentication(true, "invalidJwt");
            fail();
        } catch (ExecutionListener.AbortExecutionException aee) {
            assertTrue(aee.getMessage().contains("Invalid JWT token"));
        }
    }

    @Test
    public void givenValidExpiredJwt_whenCommandRequiredAuthentication_thenCall() throws Exception {
        doThrow(new TokenExpireException("Token is expired."))
            .when(getUnProxy(authenticationService)).validateJwtToken("validJwt");

        try {
            testRequiredAuthentication(true, "validJwt");
            fail();
        } catch (ExecutionListener.AbortExecutionException aee) {
            assertTrue(aee.getMessage().contains("Invalid JWT token"));
        }
    }

    @Test
    public void givenValidJwt_whenCommandRequiredAuthentication_thenCall() throws Exception {
        doReturn(TokenAuthentication.createAuthenticated("user", "pass"))
            .when(getUnProxy(authenticationService)).validateJwtToken("validJwt");

        AuthenticationCommand ac = testRequiredAuthentication(true, "validJwt");
        verify(ac, times(1)).apply(any());
    }

    @Test
    public void givenServiceIdAndJwt_whenExpiringCommand_thenReturnNewOne() {
        AbstractAuthenticationScheme scheme = mock(AbstractAuthenticationScheme.class);
        Application application = createApplication(
            createInstanceInfo("instanceId", AuthenticationScheme.HTTP_BASIC_PASSTICKET, "applid")
        );
        doReturn(application).when(discoveryClient).getApplication("serviceId");
        doReturn(scheme).when(authenticationSchemeFactory).getSchema(any());
        AuthenticationCommandTest cmd = new AuthenticationCommandTest(false);
        doReturn(cmd).when(scheme).createCommand(any(), any());

        // first time, create and put into cache
        assertSame(cmd, serviceAuthenticationService.getAuthenticationCommand("serviceId", "jwt"));
        verify(scheme, times(1)).createCommand(any(), any());

        // second time, get from cache
        assertSame(cmd, serviceAuthenticationService.getAuthenticationCommand("serviceId", "jwt"));
        verify(scheme, times(1)).createCommand(any(), any());

        // command expired, take new one
        cmd.setExpired(true);
        AuthenticationCommand cmd2 = new AuthenticationCommandTest(false);
        reset(scheme);
        doReturn(cmd2).when(scheme).createCommand(any(), any());
        assertSame(cmd2, serviceAuthenticationService.getAuthenticationCommand("serviceId", "jwt"));
        verify(scheme, times(1)).createCommand(any(), any());

        // second command is cached now
        assertSame(cmd2, serviceAuthenticationService.getAuthenticationCommand("serviceId", "jwt"));
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
        public boolean isRequiredValidJwt() {
            return false;
        }

    }

}
