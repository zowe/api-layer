/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { Component } from 'react';
import {
    Button,
    Checkbox,
    FormControl,
    FormControlLabel,
    FormHelperText,
    InputLabel,
    MenuItem,
    Select,
    Input,
    Tooltip,
} from '@material-ui/core';
import DeleteIcon from '@material-ui/icons/Delete';

class WizardInputs extends Component {
    constructor(props) {
        super(props);
        this.state = {};
        this.handleInputChange = this.handleInputChange.bind(this);
        this.addFields = this.addFields.bind(this);
        this.addFieldsToCurrentCategory = this.addFieldsToCurrentCategory.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
    }

    /**
     * When users fills out an input the inputData object is updated with the new information
     * @param event object containing input's name, value and its data-index attr.
     */
    handleInputChange = (event, index) => {
        const { name, checked } = event.target;
        let { value } = event.target;
        const objectToChange = this.props.data;
        const arrIndex = typeof index !== 'undefined' ? index : parseInt(event.target.getAttribute('data-index'));
        const { maxLength, lowercase, regexRestriction, validUrl } = objectToChange.content[arrIndex][name];
        const prevValue = objectToChange.content[arrIndex][name].value;
        if (name === 'serviceId') {
            this.props.updateServiceId(value);
        }
        // if prevValues was a boolean then we are handling a checkbox
        if (typeof prevValue === 'boolean') {
            value = checked;
        } else {
            value = this.applyRestrictions(maxLength, value, lowercase);
            if (value.length > 0) {
                objectToChange.content[arrIndex][name].empty = false;
            }
            objectToChange.content[arrIndex][name].problem = this.checkRestrictions(
                objectToChange.content[arrIndex][name],
                value,
                regexRestriction,
                validUrl
            );
        }
        const arr = [...objectToChange.content];
        arr[arrIndex] = {
            ...arr[arrIndex],
            [name]: { ...objectToChange.content[arrIndex][name], value, interactedWith: true },
        };
        this.updateDataWithNewContent(objectToChange, arr);
        this.propagateToMinions(name, value, arrIndex);
        this.props.validateInput(objectToChange.nav, true);
    };

    /**
     * Check if there are any minions attached and update them if a shared value has been modified
     * @param name key of the value that has been changed
     * @param value the new value
     * @param arrIndex index of the set
     */
    propagateToMinions(name, value, arrIndex) {
        const { minions } = this.props.data;
        if (minions) {
            if (Object.values(minions)[0].includes(name)) {
                let category;
                this.props.inputData.forEach((cat) => {
                    if (cat.text === Object.keys(this.props.data.minions)[0]) {
                        category = { ...cat };
                    }
                });
                if (typeof category !== 'undefined') {
                    const arr = [...category.content];
                    arr[arrIndex] = {
                        ...arr[arrIndex],
                        [name]: { ...category.content[arrIndex][name], value },
                    };
                    this.props.updateWizardData({ ...category, content: arr });
                }
            }
        }
    }

    /**
     * Check the non-applicable restrictions
     * @param inputObject one input object
     * @param value user's input
     * @param regexRestriction restriction in regex expression
     * @param validUrl whether the value should be a valid URL
     * @returns {boolean} true if there's a problem
     */
    checkRestrictions(inputObject, value, regexRestriction, validUrl) {
        let problem = false;
        if (regexRestriction !== undefined) {
            regexRestriction.forEach((regex) => {
                const restriction = new RegExp(regex.value);
                if (!restriction.test(value)) {
                    inputObject.tooltip = regex.tooltip;
                    problem = true;
                }
            });
        }
        if (validUrl) {
            try {
                // eslint-disable-next-line no-new
                new URL(value);
                problem = problem || false;
                return problem;
            } catch {
                inputObject.tooltip = 'The URL has to be valid, example: https://localhost:10014';
                return true;
            }
        }
        return problem;
    }

    /**
     * Apply any restrictions to the inputs
     * @param maxLength maximum length of the string entered. Takes first maxLength chars if exceeded.
     * @param value user's input
     * @param lowercase force the input to be lowercase
     * @returns {string} user's modified input
     */
    applyRestrictions(maxLength, value, lowercase) {
        let result = value;
        if (typeof maxLength === 'number' && parseInt(value.length) > maxLength) {
            result = value.substring(0, maxLength);
        }
        if (lowercase) {
            result = result.toLowerCase();
        }
        return result;
    }

    /**
     * Alter fields if there's interference
     */
    interferenceInjection(payload) {
        const { tiles } = this.props;
        let { data } = this.props;
        if (data.interference === 'catalog') {
            const { arr } = this.fillCategoryFromATile(tiles, payload.title, data);
            arr[0] = {
                ...arr[0],
                type: { ...arr[0].type, value: payload.title },
            };
            this.updateDataWithNewContent(data, arr);
        } else if (data.interference === 'staticCatalog') {
            this.props.inputData.forEach((category) => {
                if (category.text === 'Catalog UI Tiles') {
                    data = category;
                }
            });
            const { arr, id } = this.fillCategoryFromATile(tiles, payload.title, data);
            this.updateDataWithNewContent(data, arr);
            const currCategory = this.props.data.content;
            currCategory[0] = {
                ...currCategory[0],
                type: { ...currCategory[0].type, value: payload.title },
                catalogUiTileId: { ...currCategory[0].catalogUiTileId, value: id },
            };
            this.updateDataWithNewContent(this.props.data, currCategory);
        } else if (typeof this.props.data.interference === 'undefined') {
            const { name, title, index } = payload;
            this.handleInputChange({ target: { name, value: title, getAttribute: () => index } });
        }
    }

    /**
     * Fill out a category automatically from a tile object
     * @param tiles array of tile objects
     * @param title title of the tile
     * @param data category object that should be updated
     * @returns {{arr: *[], id: string}} returns the new, now filled category content and the id of the selected tile wrapped in an object
     */
    fillCategoryFromATile(tiles, title, data) {
        let selectedTile = { id: '', title: '', description: '', version: '', disabled: false };
        tiles.forEach((tile) => {
            if (tile.title === title) {
                selectedTile = tile;
                selectedTile.disabled = true;
            }
        });
        const arr = [...data.content];
        Object.keys(arr[0]).forEach((entry) => {
            if (entry !== 'type') {
                arr[0][entry] = { ...arr[0][entry], value: selectedTile[entry], disabled: selectedTile.disabled };
            }
        });
        return { arr, id: selectedTile.id };
    }

    /**
     * Select's onChange event contains only the changed value, so we create a usable event ourselves
     * @param entry each item's basic info - name value and index - we create event from that
     */
    handleSelect = (entry, values) => {
        const { index, itemKey } = values;
        const { value } = entry.target;
        this.interferenceInjection({ title: value, name: itemKey, index });
    };

    /**
     * Receives new content object/array and fires a redux action
     * @param category old data
     * @param newC new content object/array
     */
    updateDataWithNewContent(category, newC) {
        const result = {
            ...category,
            content: newC,
        };
        this.props.updateWizardData(result);
    }

    /**
     * Adds another set of config
     * @param category category which we should add the set to
     */
    addFields = (category) => {
        const myObject = category.content[0];
        const newObject = {};
        Object.keys(myObject).forEach((key) => {
            newObject[key] = { ...myObject[key] };
            newObject[key].interactedWith = false;
            if (typeof newObject[key].value !== 'boolean') {
                newObject[key].value = '';
            }
            newObject[key].question = myObject[key].question;
        });
        const contentCopy = [...category.content];
        contentCopy.push(newObject);
        let objectToChange = category;
        objectToChange = {
            ...objectToChange,
            content: contentCopy,
        };
        this.props.updateWizardData(objectToChange);
    };

    /**
     * Call addFields for current category
     */
    addFieldsToCurrentCategory() {
        this.addFields(this.props.data);
        if (this.props.data.minions) {
            this.props.inputData.forEach((category) => {
                if (category.text === Object.keys(this.props.data.minions)[0]) {
                    this.addFields(category);
                }
            });
        }
    }

    handleDelete(event) {
        this.props.validateInput(this.props.data.nav, true);
        if (!this.state[`delBtn${event.target.name}`]) {
            this.setState({ [`delBtn${event.target.name}`]: true });
        } else {
            this.props.deleteCategoryConfig(event.target.name, this.props.data.text);
            if (this.props.data.minions) {
                this.props.deleteCategoryConfig(event.target.name, Object.keys(this.props.data.minions)[0]);
            }
            this.setState({ [`delBtn${event.target.name}`]: false });
        }
    }

    /**
     * Wrapper for renderInputs, renderInputs() renders a single set, this function iterates over all sets(if multiple==true) and concatenates all arrays
     * @returns {unknown[]} array of the input elements to be rendered or null if config was invalid
     */
    loadInputs = () => {
        const dataAsObject = this.props.data;
        if (
            dataAsObject === undefined ||
            dataAsObject.content === undefined ||
            dataAsObject.content === null ||
            Object.entries(dataAsObject.content).length === 0
        ) {
            return null;
        }
        let result = [];
        let index = 0;
        dataAsObject.content.forEach((c) => {
            if (index !== 0 && typeof this.state[`delBtn${index}`] === 'undefined') {
                this.setState({ [`delBtn${index}`]: false });
            }
            result.push(
                <div key={`divider-${index}`} className="categoryConfigDivider">
                    <h5 className="categoryInnerDivider">{index === 0 ? null : `${dataAsObject.text} #${index}:`}</h5>
                    {this.renderDeleteButton(index, multiple)}
                </div>
            );
            result = result.concat(this.renderInputs(c, index));
            index += 1;
        });
        return result;
    };

    /**
     * Makes sure all inputs the given field depends on have the correct values
     * @param dependencies array of dependencies for the field
     * @param content content of the category
     * @returns {boolean} true if all dependencies are ok.
     */
    dependenciesSatisfied(dependencies, content) {
        let satisfied = true;
        Object.entries(dependencies).forEach((entry) => {
            const [key, value] = entry;
            if (typeof content[key] === 'undefined' || content[key].value !== value) {
                satisfied = false;
            }
        });
        return satisfied;
    }

    /**
     * Allow user to delete set, unless it's the first one.
     * @param index index of the set
     * @param multiple boolean holding info whether the set is one of many
     * @returns {JSX.Element|null} null or the Button for set deletion
     */
    renderDeleteButton(index, multiple) {
        if (index === 0 || !multiple) return null;
        return (
            <Button
                variant="danger"
                minimal
                size="medium"
                iconStart={!this.state[`delBtn${index}`] ? <DeleteIcon /> : undefined}
                name={index}
                onClick={this.handleDelete}
            >
                {this.state[`delBtn${index}`] ? 'Confirm' : null}
            </Button>
        );
    }

    /**
     * Dynamically creates input fields based on the content object of the category - accepts a single set
     * @param content object containing all inputs and questions for given category
     * @param index index of the given set - it multiple==false
     * @returns {unknown[]} array of the inputs to be rendered
     */
    renderInputs = (content, index = 0) => {
        const selectedData = Object.keys(content);
        let key = -1;
        return selectedData.map((itemKey) => {
            const input = content[itemKey];
            if (input.dependencies && !this.dependenciesSatisfied(input.dependencies, content)) {
                input.show = false;
                return null;
            }
            input.show = true;
            key += 1;
            if (input.hide) return null;
            return (
                <div className="entry" key={`${index}-${key}`}>
                    {this.renderInputElement(itemKey, index, input)}
                </div>
            );
        });
    };

    /**
     * Renders a single input field/checkbox/select (based on the item's possible values)
     * @param itemKey name of the input item
     * @param index index of the set
     * @param inputNode input's settings
     * @returns {JSX.Element} returns the input element
     */
    renderInputElement(itemKey, index, inputNode) {
        const { question, value, optional, options, maxLength, lowercase, tooltip, disabled } = inputNode;
        let caption = '';
        if (optional) {
            caption += 'Optional field; ';
        }
        if (lowercase) {
            caption += 'Field must be lowercase; ';
        }
        if (typeof maxLength === 'number') {
            caption += `Max length is ${maxLength} characters; `;
        }
        if (caption.length > 2) {
            caption = caption.slice(0, -2);
        } else {
            caption = undefined;
        }
        if (typeof value === 'boolean') {
            return (
                <FormControlLabel
                    control={
                        <Checkbox
                            className="wCheckBox"
                            checked={value}
                            onChange={(event) => this.handleInputChange(event, index)}
                            name={itemKey}
                        />
                    }
                    label={question}
                    labelPlacement="bottom"
                />
            );
        }
        if (Array.isArray(options)) {
            return (
                <FormControl className="formField" disabled={disabled}>
                    <InputLabel shrink>{question}</InputLabel>
                    <Select
                        id={itemKey}
                        value={value}
                        onChange={(event) => this.handleSelect(event, { index, itemKey })}
                    >
                        {options.map((entry) => (
                            <MenuItem value={entry}>{entry}</MenuItem>
                        ))}
                    </Select>
                </FormControl>
            );
        }
        let finalTooltip = tooltip;
        if (tooltip === undefined) {
            finalTooltip = 'filler';
        }
        const captionId = `my-helper${itemKey}`;
        return (
            <Tooltip className="wizardTooltip" title={finalTooltip}>
                <FormControl className="wizardFormFields" disabled={disabled}>
                    <InputLabel shrink>{question}</InputLabel>
                    <Input
                        id={itemKey}
                        name={itemKey}
                        value={value}
                        onChange={(event) => this.handleInputChange(event, index)}
                        aria-describedby={captionId}
                    />
                    <FormHelperText id={captionId}>{caption}</FormHelperText>
                </FormControl>
            </Tooltip>
        );
    }

    render() {
        const { multiple, isMinion } = this.props.data;
        return (
            <div className="wizardForm">
                {this.loadInputs()}
                {multiple && typeof isMinion === 'undefined' ? (
                    <Button onClick={this.addFieldsToCurrentCategory} style={{ borderRadius: '0.1875em' }}>
                        Add more fields
                    </Button>
                ) : null}
            </div>
        );
    }
}

export default WizardInputs;
