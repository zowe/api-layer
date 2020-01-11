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
import com.ca.mfaas.client.configuration.SpringComponentsConfiguration;
import com.ca.mfaas.client.model.Pet;
import com.ca.mfaas.client.service.PetService;
//import com.ca.mfaas.product.registry.EurekaClientWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {PetController.class}, secure = false)
@Import(value = {SpringComponentsConfiguration.class, ApplicationConfiguration.class/*, EurekaClientWrapper.class*/})
public class PetControllerPutTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void putExistingPet() throws Exception {
        int id = 1;
        String name = "Falco";
        Pet pet = new Pet((long) id, name);
        String payload = mapper.writeValueAsString(pet);
        when(petService.update(pet)).thenReturn(pet);

        this.mockMvc.perform(
            put("/api/v1/pets/" + id)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id)))
            .andExpect(jsonPath("$.name", is(name)));

        verify(petService, times(1)).update(any(Pet.class));
    }

    @Test
    public void putNotExistingPet() throws Exception {
        int id = 404;
        String name = "Falco";
        Pet pet = new Pet((long) id, name);
        String payload = mapper.writeValueAsString(pet);
        String message = String.format("The pet with id '%s' is not found.", id);
        when(petService.update(pet)).thenReturn(null);

        this.mockMvc.perform(
            put("/api/v1/pets/" + id)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(payload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.messages[?(@.messageNumber == 'CSR0001E')].messageContent", hasItem(message)));
    }

    @Test
    public void putPetWithMissingId() throws Exception {
        int id = 1;
        String idField = "id";
        String name = "Falco";
        String reason = "Id should be not null for pet update";
        Pet pet = new Pet(null, name);
        String payload = mapper.writeValueAsString(pet);
        String message = String.format("The field '%s' with the provided value ['%s'] is invalid due to: '%s'.", idField, pet.getId(), reason);

        this.mockMvc.perform(
            put("/api/v1/pets/" + id)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[?(@.messageNumber == 'CSR0004E')].messageContent", hasItem(message)));

        verify(petService, never()).update(any());
    }

    @Test
    public void putPetWithExtraField() throws Exception {
        int id = 4;
        String name = "Falco";
        String field = "extraField";
        String fieldValue = "value";
        String message = String.format("Unrecognized field '%s'", field);

        JSONObject json = new JSONObject()
            .put("id", id)
            .put("name", name)
            .put(field, fieldValue);

        this.mockMvc.perform(
            put("/api/v1/pets/" + id)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(json.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[?(@.messageNumber == 'CSR0005E')].messageContent", hasItem(message)));
    }

    @Test
    public void putPetWithInvalidId() throws Exception {
        String pathId = "invalidvalue";
        int bodyId = 1;
        String name = "Big Falco";
        Pet pet = new Pet((long) bodyId, name);
        String payload = mapper.writeValueAsString(pet);
        String message = String.format("The pet id '%s' is invalid: it is not an integer.", pathId);

        this.mockMvc.perform(
            put("/api/v1/pets/" + pathId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[?(@.messageNumber == 'CSR0003E')].messageContent", hasItem(message)));
    }

    @Test
    public void putPetWithDifferentIdInBodyAndPath() throws Exception {
        int pathId = 2;
        int bodyId = 1;
        String name = "Big Falco";
        Pet pet = new Pet((long) bodyId, name);
        String payload = mapper.writeValueAsString(pet);
        String message = String.format("The id '%s' in the URL is different from the request body id '%s'.", pathId, bodyId);

        this.mockMvc.perform(
            put("/api/v1/pets/" + pathId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[?(@.messageNumber == 'CSR0002E')].messageContent", hasItem(message)));
    }

    @Test
    public void putPetWithInvalidIdInBody() throws Exception {
        int pathId = 1;
        String idField = "id";
        String bodyId = "invalidvalue";
        String name = "Big Falco";

        JSONObject json = new JSONObject()
            .put(idField, bodyId)
            .put("name", name);
        String message = String.format("Field '%s' is in the wrong format.", idField);


        this.mockMvc.perform(
            put("/api/v1/pets/" + pathId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(json.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.messages[?(@.messageNumber == 'CSR0007E')].messageContent", hasItem(message)));
    }

}
