/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.api;

//import org.zowe.apiml.enable.config.OnboardingEnablerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {FileController.class}, secure = false)
//@ContextConfiguration(classes = OnboardingEnablerConfig.class)
public class FileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void callFileDownloadEndpoint() throws Exception {
        this.mockMvc.perform(get("/api/v1/get-file"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition","attachment;filename=api-catalog.png"));
    }
}
