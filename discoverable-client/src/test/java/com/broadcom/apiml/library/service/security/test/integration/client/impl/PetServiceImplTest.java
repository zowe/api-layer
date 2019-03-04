/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.test.integration.client.impl;

import com.broadcom.apiml.library.service.security.test.integration.client.exception.PetNotFoundException;
import com.broadcom.apiml.library.service.security.test.integration.client.model.Pet;
import com.broadcom.apiml.library.service.security.test.integration.client.service.impl.PetServiceImpl;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class PetServiceImplTest {
    @Test
    public void testPetSave() {
        PetServiceImpl petService = new PetServiceImpl();
        Long id = 1L;
        String name = "Falco";
        Pet pet = new Pet(id, name);

        Pet savedPet = petService.save(pet);

        assertThat(savedPet.getId(), is(id));
        assertThat(savedPet.getName(), is(name));
    }

    @Test
    public void testGetPetById() {
        PetServiceImpl petService = new PetServiceImpl();
        petService.init();
        Long id = 1L;
        String name = "Falco";

        Pet pet = petService.getById(id);

        assertThat(pet.getName(), is(name));
    }

    @Test
    public void testGetAll() {
        PetServiceImpl petService = new PetServiceImpl();
        petService.init();
        int expectedSize = 4;

        List<Pet> pets = petService.getAll();

        assertThat(pets.size(), is(expectedSize));
    }

    @Test
    public void testUpdateNotExistingPet() {
        Pet pet = new Pet(404L, "Not Existing");
        PetServiceImpl petService = new PetServiceImpl();

        Pet updatedPet = petService.update(pet);

        assertNull(updatedPet);
    }

    @Test
    public void testUpdateOfExistingPEt() {
        String name = "Big Falco";
        Pet pet = new Pet(1L, name);
        PetServiceImpl petService = new PetServiceImpl();
        petService.init();

        Pet updatedPet = petService.update(pet);

        assertThat(updatedPet.getName(), is(name));
    }

    @Test(expected = PetNotFoundException.class)
    public void testDeleteNotExistingPet() {
        Long id = 404L;
        PetServiceImpl petService = new PetServiceImpl();

        petService.deleteById(id);
    }

    @Test
    public void testDeleteExistingPet() {
        Long id = 1L;
        int expectedSize = 3;
        PetServiceImpl petService = new PetServiceImpl();
        petService.init();

        petService.deleteById(id);

        assertThat(petService.getAll().size(), is(expectedSize));
    }

}
