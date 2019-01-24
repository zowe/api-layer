/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.service;

import com.ca.mfaas.client.model.Pet;

import java.util.List;

/**
 * This is an example of a service class.
 */
public interface PetService {
    Pet save(Pet pet);
    Pet getById(Long id);
    List<Pet> getAll();
    Pet update(Pet pet);
    void deleteById(Long id);
}
