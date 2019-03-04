/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.client.service.impl;

import com.broadcom.apiml.library.service.response.client.exception.PetNotFoundException;
import com.broadcom.apiml.library.service.response.client.model.Pet;
import com.broadcom.apiml.library.service.response.client.service.PetService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is an example of basic implementation of {@link PetService}.
 * It uses {@link ArrayList} as storage for {@link Pet} objects.
 */
@Service("petService")
public class PetServiceImpl implements PetService {
    private final List<Pet> pets;
    private final AtomicLong counter;

    public PetServiceImpl() {
        this.pets = new ArrayList<>();
        counter = new AtomicLong(0);
    }

    /**
     * Initial setup of pet list for the message tests
     */
    @PostConstruct
    public void init() {
        pets.add(new Pet(counter.incrementAndGet(), "Falco")); // for get message test
        pets.add(new Pet(counter.incrementAndGet(), "Jeník")); // for get  message test
        pets.add(new Pet(counter.incrementAndGet(), "Molly")); // for update message test
        pets.add(new Pet(counter.incrementAndGet(), "Toby")); // for delete message test
    }

    /**
     * Adds pet to the list
     *
     * @param pet Pet with set ID
     * @return Pet
     */
    @Override
    public Pet save(Pet pet) {
        pet.setId(counter.incrementAndGet());
        pets.add(pet);
        return pet;
    }

    /**
     * Finds Pet by ID
     *
     * @param id Pet ID
     * @return Pet
     */
    @Override
    public Pet getById(Long id) {
        return pets.stream()
            .filter(pet -> pet.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets all pets from the list
     *
     * @return list of pets
     */
    @Override
    public List<Pet> getAll() {
        return pets;
    }

    /**
     * Updates Pet by ID
     *
     * @param pet Pet found by ID
     * @return Pet or null if pet is not found
     */
    @Override
    public Pet update(Pet pet) {
        Pet petToUpdate = getById(pet.getId());
        if (petToUpdate == null) {
            return null;
        }
        int index = pets.indexOf(petToUpdate);
        pets.set(index, pet);
        return pets.get(index);
    }

    /**
     * Deletes pet by ID
     *
     * @param id Pet ID
     */
    @Override
    public void deleteById(Long id) {
        if (!pets.removeIf(pet -> pet.getId().equals(id))) {
            throw new PetNotFoundException("Pet with provided id is not found", id);
        }
    }
}
