/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.springframework.cloud.commons.httpclient;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ryan Baxter
 */
@Configuration(proxyBeanMethods = false)
public class HttpClientConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "spring.cloud.httpclientfactories.apache.enabled",
			matchIfMissing = true)
	@ConditionalOnClass(HttpClient.class)
	static class ApacheHttpClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ApacheHttpClientConnectionManagerFactory connManFactory() {
			return new DefaultApacheHttpClientConnectionManagerFactory();
		}

		@Bean
		@ConditionalOnMissingBean
		public HttpClientBuilder apacheHttpClientBuilder() {
			return HttpClientBuilder.create();
		}

		@Bean
		@ConditionalOnMissingBean
		public ApacheHttpClientFactory apacheHttpClientFactory(
				HttpClientBuilder builder) {
			return new DefaultApacheHttpClientFactory(builder);
		}

	}

}
