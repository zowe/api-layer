/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApimlServerEndpointConfig extends ServerEndpointConfig.Configurator
    implements ServerEndpointConfig {

    private final String path;

    private final Endpoint endpoint;

    private List<String> protocols = new ArrayList<>();


    /**
     * Constructor with a path and an {@code jakarta.websocket.Endpoint}.
     *
     * @param path     the endpoint path
     * @param endpoint the endpoint instance
     */
    public ApimlServerEndpointConfig(String path, Endpoint endpoint) {
        Assert.hasText(path, "path must not be empty");
        Assert.notNull(endpoint, "endpoint must not be null");
        this.path = path;
        this.endpoint = endpoint;
    }


    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return new ArrayList<>();
    }

    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return new HashMap<>();
    }

    @Override
    public Class<?> getEndpointClass() {
        return this.endpoint.getClass();
    }

    @Override
    public String getPath() {
        return this.path;
    }

    public void setSubprotocols(List<String> protocols) {
        this.protocols = protocols;
    }

    @Override
    public List<String> getSubprotocols() {
        return this.protocols;
    }

    @Override
    public List<Extension> getExtensions() {
        return new ArrayList<>();
    }

    @Override
    public ServerEndpointConfig.Configurator getConfigurator() {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return (T) this.endpoint;
    }

    @Override
    public String toString() {
        return "DefaultServerEndpointConfig for path '" + getPath() + "': " + getEndpointClass();
    }
}
