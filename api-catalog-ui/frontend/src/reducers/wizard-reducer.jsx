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
    INPUT_UPDATED,
    NEXT_CATEGORY,
    READY_YAML_OBJECT,
    REMOVE_INDEX,
    SELECT_ENABLER,
    TOGGLE_DISPLAY,
    VALIDATE_INPUT,
} from '../constants/wizard-constants';
import { categoryData } from '../components/Wizard/configs/wizard_categories';
import { enablerData } from '../components/Wizard/configs/wizard_onboarding_methods';

export const wizardReducerDefaultState = {
    wizardIsOpen: false,
    enablerName: 'Static Onboarding',
    selectedCategory: 0,
    inputData: [],
    yamlObject: {},
    navsObj: {},
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
    if (categoryInfo.inArr !== undefined) {
        category.inArr = categoryInfo.inArr;
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

/**
 * Extracted method from findEmptyFieldsOfCategory, assumes content is an object
 * @param content content of the category to be checked
 * @param silent respect/override interactedWith
 * @returns {*[]} array of names of empty fields
 */
function emptyFieldsOfContent(content, silent) {
    const emptyFieldsArr = [];
    Object.entries(content).forEach(entry => {
        const [key, objValue] = entry;
        if (objValue.value.length === 0 && objValue.optional !== true && objValue.show !== false) {
            if (!silent || objValue.interactedWith) {
                objValue.empty = true;
                emptyFieldsArr.push(key);
            }
        }
    });
    return emptyFieldsArr;
}

/**
 * Find all empty fields of given category and return their names in array.
 * If multiple sets are allowed, each item inside the main array is array for the given set (their indices match).
 * @param content content of the category to be checked
 * @param silent respect/override interactedWith
 * @returns {*[]} array of arrays of names of empty fields
 */
export function findEmptyFieldsOfCategory(content, silent) {
    const result = [];
    if (Array.isArray(content)) {
        content.forEach(cont => {
            result.push(emptyFieldsOfContent(cont, silent));
        });
    } else {
        result.push(emptyFieldsOfContent(content, silent));
    }
    return result;
}

/**
 * Reducer for the Wizard Dialog
 * @param state state; contains all global variables for the wizrd reducer
 * @param action when a component fires an action its payload is unloaded here
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
                    navCategories[category.nav] = { [category.text]: [[]], silent: true, warn: false };
                } else {
                    navCategories[category.nav][category.text] = [[]];
                }
                inputData.push(category);
            });
            return { ...state, enablerName, inputData, selectedCategory: 0, navsObj: navCategories };
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
        case VALIDATE_INPUT: {
            const { navName, silent } = action.payload;
            const newObj = { ...state.navsObj };
            if (newObj[navName] === undefined) return state;
            state.inputData.forEach(category => {
                if (category.nav === navName) {
                    newObj[category.nav][category.text] = findEmptyFieldsOfCategory(category.content, silent);
                }
            });
            newObj[navName].silent = silent;
            let numOfEmptyFields = 0;
            Object.values(newObj[navName]).forEach(val => {
                if (Array.isArray(val)) {
                    // category
                    val.forEach(setArr => {
                        numOfEmptyFields += setArr.length;
                    });
                }
            });
            newObj[navName].warn = numOfEmptyFields > 0;
            return { ...state, navsObj: newObj };
        }
        default:
            return state;
    }
};

export default wizardReducer;
