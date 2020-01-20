/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.model;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class PetTest {
    @Test
    public void testSetName() {
        Pet pet = new Pet(1L, "Falco");
        String name = "Big Falco";
        pet.setName(name);

        assertThat(pet.getName(), is(name));
    }

    @Test
    public void testHashCode() {
        Pet firstPet = new Pet(1L, "Falco");
        Pet secondPet = new Pet(2L, "Molly");

        assertThat(firstPet.hashCode(), is(not(secondPet.hashCode())));
    }

    @Test
    public void testEquals() {
        Pet firstPet = new Pet(1L, "Falco");
        Pet secondPet = new Pet(2L, "Molly");

        assertFalse(firstPet.equals(secondPet));
        assertFalse(firstPet.equals(null));
    }
}
