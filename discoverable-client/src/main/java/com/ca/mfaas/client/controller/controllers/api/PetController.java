/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.controller.controllers.api;

import com.ca.mfaas.client.exception.PetIdMismatchException;
import com.ca.mfaas.client.exception.PetNotFoundException;
import com.ca.mfaas.client.model.Pet;
import com.ca.mfaas.client.model.state.Existing;
import com.ca.mfaas.client.model.state.New;
import com.ca.mfaas.client.service.PetService;
import com.ca.mfaas.rest.response.ApiMessage;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an example of the REST API controller to implement GET, POST, PUT and DELETE methods.
 * It uses the {@link Pet} object as a model and {@link PetService} as a service.
 */
@RestController
@RequestMapping("/api/v1")
@Api(
    value = "/qpi/v1/pets",
    consumes = "application/json",
    tags = {"The pet API"})
public class PetController {
    private final PetService petService;

    /**
     * Constructor for {@link PetController}.
     * @param petService service for working with {@link Pet} objects.
     */
    @Autowired
    public PetController(PetService petService) {
        this.petService = petService;
    }

    /**
     * The getAllPets method lists all existing pets
     *
     * @return A list of all existing pets
     */
    @GetMapping(
        value = "/pets",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ApiOperation(
        value = "List all existing pets",
        notes = "Returns information about all existing pets")
    @ApiResponses( value = {
        @ApiResponse(code = 200, message = "List of pets", response = Pet.class, responseContainer = "List")
    })
    public List<Pet> getAllPets() {
        List<Pet> pets = petService.getAll();
        if (pets == null) {
            return new ArrayList<>();
        }
        return pets;
    }

    /**
     * The addPet method creates a new pet
     *
     * @param pet A pet object without an ID
     * @return A new pet object with an ID
     */
    @PostMapping(
        value = "/pets",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(
        value = "Add a new pet",
        notes = "Creates a new pet",
        authorizations = {
            @Authorization(
                value = "ESM token"
            )
        })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "New created pet", response = Pet.class),
        @ApiResponse(code = 401, message = "Authentication is required", response = ApiMessage.class),
        @ApiResponse(code = 400, message = "Request object is not valid", response = ApiMessage.class)
    })
    public Pet addPet(@ApiParam(value = "Pet object that needs to be added", required = true)
                      @Validated(value = {New.class})
                      @RequestBody Pet pet) {
        return petService.save(pet);
    }

    /**
     * The gePetByID method finds a pet using an ID
     *
     * @param id The ID of an existing pet
     * @return A pet object
     */
    @GetMapping(
        value = "/pets/{id}",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ApiOperation(value = "Find pet by id", notes = "Returns a single pet",
        authorizations = {
            @Authorization(
                value = "ESM token"
            )
        })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = Pet.class),
        @ApiResponse(code = 401, message = "Authentication is required", response = ApiMessage.class),
        @ApiResponse(code = 404, message = "The pet with id is not found.", response = ApiMessage.class)
    })
    public Pet getPetById(@ApiParam(value = "Pet id to return", required = true)
                          @PathVariable("id") Long id) {
        Pet pet = petService.getById(id);
        if (pet == null) {
            throw new PetNotFoundException("Pet with provided id is not found", id);
        }
        return pet;
    }

    /**
     * The updatePetById method updates an existing pet
     *
     * @param id  The ID of an existing pet
     * @param pet The object with updated fields
     * @return The updated pet object
     */
    @PutMapping(
        value = "/pets/{id}",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Update an existing pet", notes = "Change information for an existing pet",
        authorizations = {
            @Authorization(
                value = "ESM token"
            )
        })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Pet updated", response = Pet.class),
        @ApiResponse(code = 401, message = "Authentication is required", response = ApiMessage.class),
        @ApiResponse(code = 404, message = "Pet not found", response = ApiMessage.class)
    })
    public Pet updatePetById(@ApiParam(value = "Pet id to update", required = true)
                             @PathVariable("id") Long id,
                             @ApiParam(value = "Pet object that needs to be updated", required = true)
                             @Validated(value = {Existing.class})
                             @RequestBody Pet pet) {
        if (!id.equals(pet.getId())) {
            throw new PetIdMismatchException("Id in URL is different from request body id", id, pet.getId());
        }
        Pet updatedPet = petService.update(pet);
        if (updatedPet == null) {
            throw new PetNotFoundException("Pet with provided id is not found", pet.getId());
        }
        return updatedPet;
    }

    /**
     * The deletePetById method deletes an existing pet
     *
     * @param id The ID of the existing pet to delete
     */
    @DeleteMapping(
        value = "/pets/{id}",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation(value = "Delete a pet", notes = "Removes an existing pet",
        authorizations = {
            @Authorization(
                value = "ESM token"
            )
        })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Pet updated", response = Pet.class),
        @ApiResponse(code = 401, message = "Authentication is required", response = ApiMessage.class),
        @ApiResponse(code = 403, message = "Forbidden", response = ApiMessage.class),
        @ApiResponse(code = 404, message = "Pet not found", response = ApiMessage.class)
    })
    public void deletePetById(@ApiParam(value = "Pet id to delete", required = true)
                              @PathVariable("id") Long id) {
        petService.deleteById(id);
    }
}
