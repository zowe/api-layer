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
    CHANGE_CATEGORY,
    INPUT_UPDATED,
    NEXT_CATEGORY,
    READY_YAML_OBJECT,
    REMOVE_INDEX,
    SELECT_ENABLER,
    TOGGLE_DISPLAY,
} from '../constants/wizard-constants';
import { categoryData } from '../components/Wizard/configs/wizard_categories';
import { enablerData } from '../components/Wizard/configs/wizard_onboarding_methods';

export const wizardReducerDefaultState = {
    wizardIsOpen: false,
    enablerName: 'Static Onboarding',
    selectedCategory: 0,
    inputData: [],
    yamlObject: {},
};

function compareVariables(category, categoryInfo) {
    if (categoryInfo.indentation !== undefined) {
        category.indentation = categoryInfo.indentation;
    }
    if (categoryInfo.multiple !== undefined) {
        category.multiple = categoryInfo.multiple;
    }
    if (category.multiple && !Array.isArray(category.content)) {
        const arr = [];
        arr.push(category.content);
        category.content = arr;
    }
}

export function addDefaultValues(content, defaultsArr) {
    const newContent = { ...content };
    defaultsArr.forEach(entry => {
        const key = entry[0];
        const defaultValue = entry[1];
        if (newContent[key]) {
            newContent[key].value = defaultValue;
        }
    });
    return newContent;
}

export function setDefault(category, defaults) {
    if (defaults === undefined || defaults[category.text] === undefined) {
        return category;
    }
    let result;
    const defaultsArr = Object.entries(defaults[category.text]);
    if (Array.isArray(category.content)) {
        result = [];
        category.content.forEach(config => result.push(addDefaultValues(config, defaultsArr)));
    } else {
        result = addDefaultValues(category.content, defaultsArr);
    }
    return { ...category, content: result };
}

const wizardReducer = (
    state = wizardReducerDefaultState,
    action = {},
    config = {
        data: categoryData,
        enablerData,
    }
) => {
    if (action == null) {
        return state;
    }
    switch (action.type) {
        case TOGGLE_DISPLAY:
            return {
                ...state,
                wizardIsOpen: !state.wizardIsOpen,
            };
        case SELECT_ENABLER: {
            const inputData = [];
            const { enablerName } = action.payload;
            const enablerObj = config.enablerData.find(o => o.text === enablerName);
            if (enablerObj === undefined || enablerObj.categories === undefined) {
                return { ...state, enablerName };
            }
            const { categories } = enablerObj;
            categories.forEach(categoryInfo => {
                let category = config.data.find(o => o.text === categoryInfo.name);
                if (category === undefined) {
                    return;
                }
                category = setDefault(category, enablerObj.defaults);
                compareVariables(category, categoryInfo);
                inputData.push(category);
            });
            return { ...state, enablerName, inputData };
        }

        case INPUT_UPDATED: {
            const { category } = action.payload;
            const inputData = state.inputData.map(group => {
                if (group.text === category.text) {
                    return category;
                }
                return group;
            });
            return { ...state, inputData };
        }
        case NEXT_CATEGORY:
            return { ...state, selectedCategory: state.selectedCategory + 1 };
        case CHANGE_CATEGORY:
            return { ...state, selectedCategory: action.payload.category };
        case READY_YAML_OBJECT:
            return { ...state, yamlObject: action.payload.yaml };
        case REMOVE_INDEX: {
            const { index, text } = action.payload;
            const newData = state.inputData.map(element => {
                const newElement = { ...element };
                if (newElement.text === text) {
                    const newArr = [...newElement.content];
                    newArr.splice(parseInt(index), 1);
                    newElement.content = newArr;
                }
                return newElement;
            });
            return { ...state, inputData: newData };
        }
        default:
            return state;
    }
};

export default wizardReducer;
