/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import {
    SELECT_ENABLER,
    TOGGLE_DISPLAY,
    INPUT_UPDATED,
    NEXT_CATEGORY,
    CHANGE_CATEGORY,
    READY_YAML_OBJECT,
} from '../constants/wizard-constants';

export function updateWizardData(category) {
    return {
        type: INPUT_UPDATED,
        payload: { category },
    };
}
export function wizardToggleDisplay() {
    return {
        type: TOGGLE_DISPLAY,
        payload: null,
    };
}
export function selectEnabler(enablerName) {
    return {
        type: SELECT_ENABLER,
        payload: { enablerName },
    };
}
export function nextWizardCategory() {
    return {
        type: NEXT_CATEGORY,
        payload: null,
    };
}
export function changeWizardCategory(num) {
    return {
        type: CHANGE_CATEGORY,
        payload: { category: num },
    };
}
export const insert = (parent, content) => {
    const keys = Object.keys(content);
    keys.forEach(currKey => {
        if (parent[currKey] === undefined) {
            parent[currKey] = content[currKey];
        } else {
            insert(parent[currKey], content[currKey]);
        }
    });
};
export const addCategoryToYamlObject = (category, result) => {
    let content = {};
    // load user's answer into content object
    if (!Array.isArray(category.content)) {
        Object.keys(category.content).forEach(key => {
            content[key] = category.content[key].value;
        });
    } else {
        content = [];
        let index = 0;
        category.content.forEach(o => {
            content[index] = {};
            Object.keys(o).forEach(key => {
                content[index][key] = category.content[index][key].value;
            });
            index += 1;
        });
    }
    // handle indentation, if any
    if (!category.indentation) {
        insert(result, content);
    } else {
        const indent = category.indentation;
        const arr = indent.split('/');
        arr.reverse().forEach(key => {
            if (key.length > 0)
                content = {
                    [key]: content,
                };
        });
        insert(result, content);
    }
    // return result
    return result;
};
export function createYamlObject(inputData) {
    let result = {};
    inputData.forEach(category => {
        result = addCategoryToYamlObject(category, result);
    });
    return {
        type: READY_YAML_OBJECT,
        payload: { yaml: result },
    };
}
