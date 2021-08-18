/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import _ from 'lodash';
import {
    CHANGE_CATEGORY,
    CHECK_INPUT,
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
    navTabArray: {},
};

/**
 * Override multiple and indentation properties if the enabler asks to
 * @param category category object
 * @param categoryInfo enabler's category config
 */
function compareVariables(category, categoryInfo) {
    if (categoryInfo.nav === undefined) {
        categoryInfo.nav = categoryInfo.name;
    }
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

/**
 * For each value present in enabler's defaults add its predetermined value to the content of the correct category
 * @param content content object
 * @param defaultsArr array of [key, value] arrays
 */
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

/**
 * Checks for invalid configurations, also handles situations where the content is an array instead of an object
 * @param category category object
 * @param defaults defaults object; these are defined in wizard_defaults
 */
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

export function checkInput(content) {
    let empty = false;
    const values = Object.values(content);
    let index = 0;
    while (index < values.length) {
        if (!empty && values[index].optional === undefined) {
            empty = values[index].value.length === 0;
        }
        if (values[index].optional === true) {
            values[index].empty = false;
        } else {
            values[index].empty = values[index].value.length === 0;
        }
        index += 1;
    }
    return empty;
}

export function emptyNav(navsObject, navName, unfilled) {
    Object.entries(navsObject).forEach(nav => {
        if (navName === nav[0]) {
            navsObject[nav[0]] = { emptyField: unfilled };
        }
    });
    return navsObject;
}

/**
 * Reducer for the Wizard Dialog
 * @param state state; contains all global variables for the wizrd reducer
 * @param action when a component fires an action it's payload is unloaded here
 * @param config additional configuration
 * @returns {any}
 */
const wizardReducer = (state = wizardReducerDefaultState, action = {}, config = { categoryData, enablerData }) => {
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
            const navCategories = {};
            const { enablerName } = action.payload;
            const enablerObj = config.enablerData.find(o => o.text === enablerName);
            if (enablerObj === undefined || enablerObj.categories === undefined) {
                return { ...state, enablerName };
            }
            const { categories } = enablerObj;
            categories.forEach(categoryInfo => {
                let category = config.categoryData.find(o => o.text === categoryInfo.name);
                if (category === undefined) {
                    return;
                }
                category = _.cloneDeep(category);
                category = setDefault(category, enablerObj.defaults);
                compareVariables(category, categoryInfo);
                category.nav = categoryInfo.nav;
                if (!(category.nav in navCategories)) {
                    navCategories[category.nav] = {};
                }
                inputData.push(category);
            });
            return { ...state, enablerName, inputData, selectedCategory: 0, navTabArray: navCategories };
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
        case CHECK_INPUT: {
            const { navName } = action.payload;
            let unfilled;
            let navArr = state.navTabArray;
            state.inputData.forEach(category => {
                if (category.nav === navName) {
                    if (!Array.isArray(category.content)) {
                        unfilled = checkInput(category.content) || unfilled;
                        navArr = emptyNav(navArr, navName, unfilled);
                    } else {
                        category.content.forEach(categoryContentSet => {
                            unfilled = checkInput(categoryContentSet) || unfilled;
                            navArr = emptyNav(navArr, navName, unfilled);
                        });
                    }
                }
            });
            return { ...state, navTabArray: navArr };
        }
        default:
            return state;
    }
};

export default wizardReducer;
