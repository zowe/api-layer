/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml;
//import org.junit.Test;
//import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.junit4.SpringRunner;

@EnableAutoConfiguration
@EnableConfigurationProperties
//@RunWith(SpringRunner.class)
@TestPropertySource(locations = {"classpath:/application.yml"})
public class DiscoverableClientSampleApplicationTests {

    /*@Test
    public void contextLoads() {
    }*/

}
