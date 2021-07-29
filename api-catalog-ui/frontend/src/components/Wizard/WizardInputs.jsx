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
import { FormField } from 'mineral-ui';
import TextInput from 'mineral-ui/TextInput';
import Button from 'mineral-ui/Button';

class WizardInputs extends Component {
    constructor(props) {
        super(props);
        this.handleInputChange = this.handleInputChange.bind(this);
        this.addFields = this.addFields.bind(this);
    }

    handleInputChange = event => {
        const { name, value } = event.target;
        let objectToChange = this.props.data;
        if (!objectToChange.multiple) {
            const { question } = objectToChange.content[name];
            objectToChange = {
                ...objectToChange,
                content: { ...objectToChange.content, [name]: { value, question } },
            };
            this.props.updateWizardData(objectToChange);
        } else {
            const arrIndex = parseInt(event.target.getAttribute('data-index'));
            const { question } = objectToChange.content[arrIndex][name];
            const arr = [...objectToChange.content];
            arr[arrIndex] = { ...arr[arrIndex], [name]: { question, value } };
            objectToChange = {
                ...objectToChange,
                content: arr,
            };
            this.props.updateWizardData(objectToChange);
        }
    };

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
                    <h5 key={`divider-${index}`} className="categoryInnerDivider">
                        {dataAsObject.text} #{index}:
                    </h5>
                );
                result = result.concat(this.renderInputs(c, index));
                index += 1;
            });
            return result;
        }
        return this.renderInputs(dataAsObject.content, 1);
    };

    renderInputs = (content, index) => {
        const selectedData = Object.keys(content);
        let key = 1;
        return selectedData.map(itemKey => {
            key += 1;
            const { question, value } = content[itemKey];
            return (
                <div className="entry" key={`${index}-${key}`}>
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
                </div>
            );
        });
    };

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
