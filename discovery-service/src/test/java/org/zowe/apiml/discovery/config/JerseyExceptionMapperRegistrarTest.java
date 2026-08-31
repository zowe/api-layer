/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import jakarta.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class JerseyExceptionMapperRegistrarTest {

    @Test
    void givenResourceConfigBean_whenPostProcessAfterInitialization_thenAllExceptionMappersAreRegistered() {
        ExceptionMapper mapperOne = mock(ExceptionMapper.class);
        ExceptionMapper mapperTwo = mock(ExceptionMapper.class);
        ObjectProvider<ExceptionMapper> exceptionMappers = mockProvider(List.of(mapperOne, mapperTwo));
        JerseyExceptionMapperRegistrar registrar = new JerseyExceptionMapperRegistrar(exceptionMappers);
        ResourceConfig resourceConfig = mock(ResourceConfig.class);

        Object result = registrar.postProcessAfterInitialization(resourceConfig, "jerseyApplication");

        assertThat(result).isSameAs(resourceConfig);
        verify(resourceConfig).register(mapperOne);
        verify(resourceConfig).register(mapperTwo);
    }

    @Test
    void givenUnrelatedBean_whenPostProcessAfterInitialization_thenBeanIsReturnedUnchangedAndNothingRegistered() {
        ObjectProvider<ExceptionMapper> exceptionMappers = mock(ObjectProvider.class);
        JerseyExceptionMapperRegistrar registrar = new JerseyExceptionMapperRegistrar(exceptionMappers);
        Object bean = new Object();

        Object result = registrar.postProcessAfterInitialization(bean, "someOtherBean");

        assertThat(result).isSameAs(bean);
    }

    private ObjectProvider<ExceptionMapper> mockProvider(List<ExceptionMapper> mappers) {
        ObjectProvider<ExceptionMapper> provider = mock(ObjectProvider.class);
        doAnswer(invocation -> {
            Consumer<ExceptionMapper> action = invocation.getArgument(0);
            mappers.forEach(action);
            return null;
        }).when(provider).forEach(any());
        return provider;
    }

}
