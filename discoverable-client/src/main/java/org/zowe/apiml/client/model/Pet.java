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

import org.zowe.apiml.client.model.state.Existing;
import org.zowe.apiml.client.model.state.New;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import java.util.Objects;

/**
 * This is an example of a model class.
 */
@ApiModel(description = "A Pet Object")
public class Pet {
    @Null(groups = New.class, message = "Id should be null for pet creation")
    @NotNull(groups = Existing.class, message = "Id should be not null for pet update")
    @ApiModelProperty(value = "The id is of the pet", example = "1")
    private Long id;

    @NotEmpty(groups = { New.class, Existing.class }, message = "Name should not be empty string")
    @ApiModelProperty(value = "The name of the pet", example = "Falco")
    private String name;

    /**
     * Pet object
     * @param id Pet ID
     * @param name Pet name
     */
    public Pet(@JsonProperty("id") Long id,
               @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets Pet ID
     * @return Pet ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets Pet name
     * @return Pet name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets Pet ID
     * @param id Pet ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Sets Pet name
     * @param name Pet name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pet pet = (Pet) o;
        return Objects.equals(id, pet.id) &&
            Objects.equals(name, pet.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
