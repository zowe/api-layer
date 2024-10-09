/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/**
 * each new category:
 * 1. must contain properties:
 *  1.1. text - the name of the category
 *  1.2. content - object containing sub-objects (each with a value and a question key)
 * 2. can contain properties:
 *  2.1. multiple - boolean, if true, allows for multiple sets of configuration
 *  2.2. indentation - string, nests object like so: 'a/b' - { a:{ b:your_object } }
 */
import { baseCategories } from './wizard_base_categories';
import { springSpecificCategories } from './wizard_spring_categories';
import { staticSpecificCategories } from './wizard_static_categories';
import { nodeSpecificCategories } from './wizard_node_categories';
import { micronautSpecificCategories } from './wizard_micronaut_categories';
export const categoryData = [
    ...baseCategories,
    ...springSpecificCategories,
    ...staticSpecificCategories,
    ...nodeSpecificCategories,
    ...micronautSpecificCategories,
];
