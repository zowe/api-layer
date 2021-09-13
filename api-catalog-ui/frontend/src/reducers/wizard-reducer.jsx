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
    UPDATE_SERVICE_ID,
    VALIDATE_INPUT,
    WIZARD_VISIBILITY_TOGGLE,
    OVERRIDE_DEF,
} from '../constants/wizard-constants';
import { categoryData } from '../components/Wizard/configs/wizard_categories';
import { enablerData } from '../components/Wizard/configs/wizard_onboarding_methods';

export const wizardReducerDefaultState = {
    userCanAutoOnboard: false,
    wizardIsOpen: false,
    enablerName: '',
    selectedCategory: 0,
    inputData: [],
    yamlObject: {},
    navsObj: {},
    serviceId: '',
    confirmDialog: false,
};

/**
 * Override properties if the enabler specifies
 * @param category category object
 * @param categoryInfo enabler's category config
 */
export function compareVariables(category, categoryInfo) {
    if (categoryInfo.nav === undefined) {
        categoryInfo.nav = categoryInfo.name;
    }
    const overridingValues = ['indentation', 'multiple', 'inArr', 'arrIndent', 'minions'];
    overridingValues.forEach(overrideKey => {
        if (categoryInfo[overrideKey] !== undefined) {
            category[overrideKey] = categoryInfo[overrideKey];
        }
    });
    if (!Array.isArray(category.content) && category.content !== undefined) {
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
        const defaultValueObj = entry[1];
        if (newContent[key]) {
            newContent[key].value = defaultValueObj.value;
            if (defaultValueObj.hide !== undefined) {
                newContent[key].hide = defaultValueObj.hide;
            }
        }
    });
    return newContent;
}

/**
 * Checks for invalid configurations, also feeds
 * @param category category object
 * @param defaults defaults object; these are defined in wizard_defaults
 */
export function setDefault(category, defaults) {
    if (defaults === undefined || defaults[category.text] === undefined) {
        return category;
    }
    const result = [];
    const defaultsArr = Object.entries(defaults[category.text]);
    category.content.forEach(config => result.push(addDefaultValues(config, defaultsArr)));
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
    content.forEach(cont => {
        result.push(emptyFieldsOfContent(cont, silent));
    });
    return result;
}

/**
 * Tell minions they are minions
 * @param minions contains array of objects - each object has a key of the minion category & the inputs that should be disabled
 * @param inputData inputData
 */
function warnMinions(minions, inputData) {
    inputData.forEach(category => {
        minions.forEach(entry => {
            if (category.text === entry.key) {
                category.isMinion = true;
                entry.inputsToBeDisabled.forEach(inputName => {
                    category.content[0][inputName].disabled = true;
                });
            }
        });
    });
}

/**
 * Alter fields of given category on initial onboarding method creation
 * @param category category to be affected
 * @param payload additional payload
 */
export function affectCategory(category, payload) {
    if (category.interference === 'catalog') {
        const { tiles } = payload;
        const arr = [...category.content];
        arr[0] = { ...arr[0], type: { ...arr[0].type, options: arr[0].type.options.concat(tiles) } };
        return { ...category, content: arr };
    }
    return category;
}

/**
 * Load categories according to the enabler needs
 * @param enablerObj enabler config
 * @param config category & enabler data
 * @param payload additional payload from the enabler
 * @returns {any} inputData and navCategories wrapped in an object
 */
function loadCategories(enablerObj, config, payload) {
    const minions = [];
    const inputData = [];
    const navCategories = {};
    const { categories } = enablerObj;
    categories.forEach(categoryInfo => {
        let category = config.categoryData.find(o => o.text === categoryInfo.name);
        if (category === undefined) {
            return;
        }
        category = _.cloneDeep(category);
        compareVariables(category, categoryInfo);
        category = setDefault(category, enablerObj.defaults);
        category.nav = categoryInfo.nav;
        // if category has minions, add them to the array so warnMinions() can warn them
        if (typeof category.minions !== 'undefined') {
            minions.push({
                key: Object.keys(category.minions)[0],
                inputsToBeDisabled: Object.values(category.minions)[0],
            });
        }
        category = affectCategory(category, payload);
        if (!(category.nav in navCategories)) {
            navCategories[category.nav] = { [category.text]: [[]], silent: true, warn: false };
        } else {
            navCategories[category.nav][category.text] = [[]];
        }
        inputData.push(category);
    });
    warnMinions(minions, inputData);
    return { inputData, navCategories };
}

/**
 * Make sure all mandatory fields of the given nav are filled.
 * @param inputData contains user's input
 * @param navName name of the selected nav
 * @param navsObj contains info about the missing fields in different navs
 * @param silent respect/override interactedWith
 * @returns {*} updated navsObj
 */
function checkPresence(inputData, navName, navsObj, silent) {
    const newObj = { ...navsObj };
    inputData.forEach(category => {
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
    return newObj;
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
        case WIZARD_VISIBILITY_TOGGLE:
            return {
                ...state,
                userCanAutoOnboard: action.payload.state,
            };
        case TOGGLE_DISPLAY:
            return {
                ...state,
                wizardIsOpen: !state.wizardIsOpen,
            };
        case SELECT_ENABLER: {
            const { enablerName } = action.payload;
            const enablerObj = config.enablerData.find(o => o.text === enablerName);
            if (enablerObj === undefined || enablerObj.categories === undefined) {
                return { ...state, enablerName };
            }
            const { inputData, navCategories } = loadCategories(enablerObj, config, action.payload);
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
            if (state.navsObj[navName] === undefined) return state;
            const navsObj = checkPresence(state.inputData, navName, state.navsObj, silent);
            return { ...state, navsObj };
        }
        case UPDATE_SERVICE_ID: {
            return { ...state, serviceId: action.payload.value };
        }
        case OVERRIDE_DEF: {
            return { ...state, confirmDialog: !state.confirmDialog, wizardIsOpen: !state.wizardIsOpen };
        }
        default:
            return state;
    }
};

export default wizardReducer;
