/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import React, { Component } from 'react';
import { Checkbox, FormField, Select } from 'mineral-ui';
import TextInput from 'mineral-ui/TextInput';
import Button from 'mineral-ui/Button';
import { IconDelete } from 'mineral-ui-icons';

class WizardInputs extends Component {
    constructor(props) {
        super(props);
        this.handleInputChange = this.handleInputChange.bind(this);
        this.addFields = this.addFields.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
    }

    /**
     * When users fills out an input the inputData object is updated with the new information
     * @param event object containing input's name, value and its data-index attr.
     */
    handleInputChange = event => {
        const { name, checked } = event.target;
        let { value } = event.target;
        const objectToChange = this.props.data;
        if (!objectToChange.multiple) {
            const { question, maxLength, lowercase } = objectToChange.content[name];
            value = this.applyRestrictions(maxLength, value, lowercase);
            const prevValue = objectToChange.content[name].value;
            // if prevValues was a boolean then we are handling a checkbox
            if (typeof prevValue === 'boolean') {
                value = checked;
            }
            const newContent = {
                ...objectToChange.content,
                [name]: { ...objectToChange.content[name], value, question },
            };
            this.updateDataWithNewContent(objectToChange, newContent);
        } else {
            const arrIndex = parseInt(event.target.getAttribute('data-index'));
            const { question, maxLength, lowercase } = objectToChange.content[arrIndex][name];
            value = this.applyRestrictions(maxLength, value, lowercase);
            const arr = [...objectToChange.content];
            arr[arrIndex] = { ...arr[arrIndex], [name]: { ...objectToChange.content[name], question, value } };
            this.updateDataWithNewContent(objectToChange, arr);
        }
    };

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
     * Select's onChange event contains only the changed value, so we create a usable event ourselves
     * @param entry each item's basic info - name value and index - we create event from that
     */
    handleSelect = entry => {
        const { name, value, index } = entry;
        this.handleInputChange({ target: { name, value, getAttribute: () => index } });
    };

    /**
     * Receives new content object/array and fires a redux action
     * @param objectToChange old data
     * @param newC new content object/array
     */
    updateDataWithNewContent(objectToChange, newC) {
        const result = {
            ...objectToChange,
            content: newC,
        };
        this.props.updateWizardData(result);
    }

    /**
     * Adds another set of config if the category's multiple property is set to true
     */
    addFields = () => {
        const myObject = this.props.data.content[0];
        const newObject = {};
        Object.keys(myObject).forEach(key => {
            newObject[key] = {};
            newObject[key].value = '';
            newObject[key].question = myObject[key].question;
        });
        const contentCopy = [...this.props.data.content];
        contentCopy.push(newObject);
        let objectToChange = this.props.data;
        objectToChange = {
            ...objectToChange,
            content: contentCopy,
        };
        this.props.updateWizardData(objectToChange);
    };

    handleDelete(event) {
        this.props.deleteCategoryConfig(event.target.name, this.props.data.text);
    }

    /**
     * Wrapper for renderInputs, renderInputs() renders a single set, this function iterates over all sets(if multiple==true) and concatenates all arrays
     * @returns {unknown[]} array of the input elements to be rendered or null if config was invalid
     */
    loadInputs = () => {
        const dataAsObject = this.props.data;
        const { multiple } = this.props.data;
        if (
            dataAsObject === undefined ||
            dataAsObject.content === undefined ||
            dataAsObject.content === null ||
            Object.entries(dataAsObject.content).length === 0
        ) {
            return null;
        }
        if (multiple) {
            let result = [];
            let index = 0;
            dataAsObject.content.forEach(c => {
                result.push(
                    <div key={`divider-${index}`} className="categoryConfigDivider">
                        <h5 className="categoryInnerDivider">
                            {dataAsObject.text} #{index}:
                        </h5>
                        {index === 0 ? null : (
                            <Button
                                variant="danger"
                                minimal
                                size="medium"
                                iconStart={<IconDelete />}
                                name={index}
                                onClick={this.handleDelete}
                            />
                        )}
                    </div>
                );
                result = result.concat(this.renderInputs(c, index));
                index += 1;
            });
            return result;
        }
        return this.renderInputs(dataAsObject.content);
    };

    dependenciesSatisfied(dependencies, content) {
        let satisfied = true;
        Object.entries(dependencies).forEach(entry => {
            const [key, value] = entry;
            if (content[key].value !== value) {
                satisfied = false;
            }
        });
        return satisfied;
    }

    /**
     * Dynamically creates input fields based on the content object of the category - accepts a single set
     * @param content object containing all inputs and questions for given category
     * @param index index of the given set - it multiple==false
     * @returns {unknown[]} array of the inputs to be rendered
     */
    renderInputs = (content, index = 1) => {
        const selectedData = Object.keys(content);
        let key = 1;
        return selectedData.map(itemKey => {
            const input = content[itemKey];
            if (input.dependencies && !this.dependenciesSatisfied(input.dependencies, content)) {
                input.show = false;
                return null;
            }
            input.show = true;
            key += 1;
            return (
                <div className="entry" key={`${index}-${key}`}>
                    {this.renderInputElement(itemKey, index, input)}
                </div>
            );
        });
    };

    renderInputElement(itemKey, index, inputNode) {
        const { question, value, empty, optional, options, maxLength, lowercase } = inputNode;
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
                <Checkbox
                    className="wCheckBox"
                    label={question}
                    checked={value}
                    onChange={this.handleInputChange}
                    name={itemKey}
                    data-index={index}
                    labelPosition="start"
                    justify
                />
            );
        }
        if (Array.isArray(options)) {
            return (
                <FormField
                    input={Select}
                    size="large"
                    placeholder={itemKey}
                    selectedItem={{ text: value }}
                    label={question}
                    variant={empty ? 'danger' : undefined}
                    caption={caption}
                    data={options.map(entry => ({
                        text: entry,
                        onClick: () => this.handleSelect({ name: itemKey, index, value: entry }),
                    }))}
                />
            );
        }
        return (
            <FormField
                input={TextInput}
                size="large"
                name={itemKey}
                onChange={this.handleInputChange}
                data-index={index}
                placeholder={itemKey}
                value={value}
                label={question}
                variant={empty ? 'danger' : undefined}
                caption={caption}
            />
        );
    }

    render() {
        return (
            <div className="wizardForm">
                {this.loadInputs()}
                {this.props.data.multiple ? <Button onClick={this.addFields}>Add more fields</Button> : null}
            </div>
        );
    }
}

export default WizardInputs;
