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

class WizardInputs extends Component {
    constructor(props) {
        super(props);
        this.handleInputChange = this.handleInputChange.bind(this);
    }

    handleInputChange(event) {
        const { name, value } = event.target;
        let objectToChange = this.props.data;
        const { question } = objectToChange.content[name];
        objectToChange = {
            ...objectToChange,
            content: { ...objectToChange.content, [name]: { value, question } },
        };
        this.props.updateWizardData(objectToChange);
    }

    loadInputs = () => {
        const dataAsObject = this.props.data;
        if (
            dataAsObject === undefined ||
            dataAsObject.content === undefined ||
            dataAsObject.content === null ||
            Object.entries(dataAsObject.content).length === 0
        ) {
            return '';
        }
        const selectedData = Object.keys(dataAsObject.content);
        let key = 0;
        return selectedData.map(itemKey => {
            key += 1;
            const { question, value } = dataAsObject.content[itemKey];
            return (
                <div className="entry" key={key}>
                    <FormField
                        input={TextInput}
                        size="large"
                        name={itemKey}
                        onChange={this.handleInputChange}
                        placeholder={itemKey}
                        value={value}
                        label={question}
                    />
                </div>
            );
        });
    };

    render() {
        return <div className="wizardForm"> {this.loadInputs()}</div>;
    }
}

export default WizardInputs;
