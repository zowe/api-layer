/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.api;

import com.ca.mfaas.client.configuration.ApplicationConfiguration;
import com.ca.mfaas.client.model.Pet;
import com.ca.mfaas.client.service.PetService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {PetController.class}, secure = false)
@Import(ApplicationConfiguration.class)
public class PetControllerGetAllTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;

    @Test
    public void getAllPets() throws Exception {
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
    public void getAllPetsForNoPetsFromService() throws Exception {
        when(petService.getAll()).thenReturn(new ArrayList<>());

        this.mockMvc.perform(get("/api/v1/pets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getAllPetsForNullPetsFromService() throws Exception {
        when(petService.getAll()).thenReturn(null);

        this.mockMvc.perform(get("/api/v1/pets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

}
