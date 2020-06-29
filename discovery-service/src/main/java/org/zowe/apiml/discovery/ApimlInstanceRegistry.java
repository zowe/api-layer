/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.registry.AbstractInstanceRegistry;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.resources.ServerCodecs;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistry;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This implementation of instance registry is solving known problem in Eureka. Discovery service notify about change
 * in services before it does it. From this reason listener can try to use services before they are really registered.
 * <p>
 * At least implementation with reflection of register and cancel should be removed after finish task in Eureka:
 * <p>
 * #2659 Race condition with registration events in Eureka server
 * https://github.com/spring-cloud/spring-cloud-netflix/issues/2659
 */
public class ApimlInstanceRegistry extends InstanceRegistry {

    private static final String EXCEPTION_MESSAGE = "Implementation of InstanceRegistry changed, please verify fix of order sending events";

    private MethodHandle handleRegistrationMethod;
    private MethodHandle handlerResolveInstanceLeaseDurationMethod;
    private MethodHandle handleCancelationMethod;

    private MethodHandle register2ArgsMethodHandle;
    private MethodHandle register3ArgsMethodHandle;
    private MethodHandle cancelMethodHandle;

    private ApplicationContext appCntx;

    public ApimlInstanceRegistry(
        EurekaServerConfig serverConfig,
        EurekaClientConfig clientConfig,
        ServerCodecs serverCodecs,
        EurekaClient eurekaClient,
        InstanceRegistryProperties instanceRegistryProperties,
        ApplicationContext appCntx
    ) {
        super(serverConfig, clientConfig, serverCodecs, eurekaClient,
            instanceRegistryProperties.getExpectedNumberOfClientsSendingRenews(),
            instanceRegistryProperties.getDefaultOpenForTrafficCount()
        );
        this.appCntx = appCntx;
        init();
    }


    /**
     * Prepare method handlers to overridden methods to reimplement methods in InstanceRegistry, which contains a race
     * condition problem. Handlers are faster than reflection, close to bytecode.
     */
    private void init() {
        try {
            Method registrationMethod =
                InstanceRegistry.class.getDeclaredMethod("handleRegistration",
                    InstanceInfo.class, int.class, boolean.class
                );
            registrationMethod.setAccessible(true);
            handleRegistrationMethod = MethodHandles.lookup().unreflect(registrationMethod);

            Method cancelationMethod =
                InstanceRegistry.class.getDeclaredMethod("handleCancelation",
                    String.class, String.class, boolean.class
                );
            cancelationMethod.setAccessible(true);
            handleCancelationMethod = MethodHandles.lookup().unreflect(cancelationMethod);

            Method resolveInstanceLeaseDurationMethod =
                InstanceRegistry.class.getDeclaredMethod("resolveInstanceLeaseDuration",
                    InstanceInfo.class
                );
            resolveInstanceLeaseDurationMethod.setAccessible(true);
            handlerResolveInstanceLeaseDurationMethod = MethodHandles.lookup().unreflect(resolveInstanceLeaseDurationMethod);

            Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            lookupConstructor.setAccessible(true);
            MethodHandles.Lookup lookup = lookupConstructor.newInstance(PeerAwareInstanceRegistryImpl.class);

            register2ArgsMethodHandle =
                lookup.findSpecial(
                    PeerAwareInstanceRegistryImpl.class,
                    "register",
                    MethodType.methodType(void.class, InstanceInfo.class, boolean.class),
                    PeerAwareInstanceRegistryImpl.class
                );

            cancelMethodHandle =
                lookup.findSpecial(
                    PeerAwareInstanceRegistryImpl.class,
                    "cancel",
                    MethodType.methodType(boolean.class, String.class, String.class, boolean.class),
                    PeerAwareInstanceRegistryImpl.class
                );

            lookup = lookupConstructor.newInstance(AbstractInstanceRegistry.class);

            register3ArgsMethodHandle =
                lookup.findSpecial(
                    AbstractInstanceRegistry.class,
                    "register",
                    MethodType.methodType(void.class, InstanceInfo.class, int.class, boolean.class),
                    AbstractInstanceRegistry.class
                );
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, e);
        }
    }

    protected int resolveInstanceLeaseDurationRewritten(final InstanceInfo info) {
        try {
            return (int) handlerResolveInstanceLeaseDurationMethod.invokeWithArguments(this, info);
        } catch (ClassCastException | WrongMethodTypeException e) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, e);
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, t);
        }
    }

    @Override
    public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
        try {
            register3ArgsMethodHandle.invokeWithArguments(this, info, leaseDuration, isReplication);
            handleRegistrationMethod.invokeWithArguments(this, info, leaseDuration, isReplication);
        } catch (ClassCastException | WrongMethodTypeException e) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, e);
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, t);
        }
    }

    @Override
    public void register(final InstanceInfo info, final boolean isReplication) {
        try {
            register2ArgsMethodHandle.invokeWithArguments(this, info, isReplication);
            handleRegistrationMethod.invokeWithArguments(this, info, resolveInstanceLeaseDurationRewritten(info), isReplication);
        } catch (ClassCastException | WrongMethodTypeException e) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, e);
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, t);
        }
    }

    @Override
    public boolean cancel(String appName, String serverId, boolean isReplication) {
        try {
            final boolean out = (boolean) cancelMethodHandle.invokeWithArguments(this, appName, serverId, isReplication);
            handleCancelationMethod.invokeWithArguments(this, appName, serverId, isReplication);
            return out;
        } catch (ClassCastException | WrongMethodTypeException e) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, e);
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable t) {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE, t);
        }
    }

    @Override
    public boolean statusUpdate(String appName, String instanceId, InstanceInfo.InstanceStatus newStatus, String lastDirtyTimestamp, boolean isReplication) {
        boolean isUpdated = super.statusUpdate(appName, instanceId, newStatus, lastDirtyTimestamp, isReplication);
        this.appCntx.publishEvent(new EurekaStatusUpdateEvent(this, appName, instanceId));
        return isUpdated;
    }
}
