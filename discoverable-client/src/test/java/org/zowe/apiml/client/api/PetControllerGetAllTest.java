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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.zowe.apiml.client.configuration.ApplicationConfiguration;
import org.zowe.apiml.client.configuration.SecurityConfiguration;
import org.zowe.apiml.client.model.Pet;
import org.zowe.apiml.client.service.PetService;
import org.zowe.apiml.util.config.TestConfig;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PetController.class})
@Import(value = { SecurityConfiguration.class, ApplicationConfiguration.class, TestConfig.class })
class PetControllerGetAllTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetService petService;

    @Test
    void getAllPets() throws Exception {
        int id = 1;
        String name = "Falco";
        List<Pet> pets = singletonList(new Pet((long) id, name));
        when(petService.getAll()).thenReturn(pets);

        this.mockMvc.perform(get("/api/v1/pets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id", is(id)))
            .andExpect(jsonPath("$[0].name", is(name)));
    }

    @Test
    void getAllPetsForNoPetsFromService() throws Exception {
        when(petService.getAll()).thenReturn(new ArrayList<>());

        this.mockMvc.perform(get("/api/v1/pets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllPetsForNullPetsFromService() throws Exception {
        when(petService.getAll()).thenReturn(null);

        this.mockMvc.perform(get("/api/v1/pets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

}
