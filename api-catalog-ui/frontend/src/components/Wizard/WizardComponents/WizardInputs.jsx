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
import { Checkbox, FormField } from 'mineral-ui';
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
            const { question } = objectToChange.content[name];
            const prevValue = objectToChange.content[name].value;
            // if prevValues was a boolean then we are handling a checkbox
            if (typeof prevValue === 'boolean') {
                value = checked;
            }
            const newContent = { ...objectToChange.content, [name]: { value, question } };
            this.updateDataWithNewContent(objectToChange, newContent);
        } else {
            const arrIndex = parseInt(event.target.getAttribute('data-index'));
            const { question } = objectToChange.content[arrIndex][name];
            const arr = [...objectToChange.content];
            arr[arrIndex] = { ...arr[arrIndex], [name]: { question, value } };
            this.updateDataWithNewContent(objectToChange, arr);
        }
    };

    updateDataWithNewContent(objectToChange, arr) {
        const result = {
            ...objectToChange,
            content: arr,
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
            key += 1;
            const { question, value } = content[itemKey];
            return (
                <div className="entry" key={`${index}-${key}`}>
                    {this.renderInputElement(itemKey, index, value, question)}
                </div>
            );
        });
    };

    renderInputElement(itemKey, index, value, question) {
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
