/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { baseCategories } from './wizard_base_categories';
import { springSpecificCategories } from './wizard_spring_categories';
import { staticSpecificCategories } from './wizard_static_categories';
import { nodeSpecificCategories } from './wizard_node_categories';
import { micronautSpecificCategories } from './wizard_micronaut_categories';
// eslint-disable-next-line import/prefer-default-export
export const categoryData = [
    ...baseCategories,
    ...springSpecificCategories,
    ...staticSpecificCategories,
    ...nodeSpecificCategories,
    ...micronautSpecificCategories,
];
