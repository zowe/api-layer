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

import org.zowe.apiml.client.configuration.ApplicationConfiguration;
import org.zowe.apiml.client.service.PetService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {PetController.class}, secure = false)
@Import(ApplicationConfiguration.class)
public class PetControllerDeleteTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;

    @Test
    public void deleteExistingPet() throws Exception {
        int id = 1;

        this.mockMvc.perform(delete("/api/v1/pets/" + id))
            .andExpect(status().isNoContent());

        verify(petService, times(1)).deleteById((long) id);
    }


}
