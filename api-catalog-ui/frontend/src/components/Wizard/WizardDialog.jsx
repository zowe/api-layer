/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import yaml from 'js-yaml';
import * as YAML from 'yaml';
import { Component } from 'react';
import { Dialog, DialogContent, DialogContentText, DialogTitle, DialogActions, IconButton } from '@material-ui/core';
import WizardNavigationContainer from './WizardComponents/WizardNavigationContainer';

export default class WizardDialog extends Component {
    constructor(props) {
        super(props);
        this.nextSave = this.nextSave.bind(this);
        this.showFile = this.showFile.bind(this);
        this.fillInputs = this.fillInputs.bind(this);
        this.renderDoneButtonText = this.renderDoneButtonText.bind(this);
    }

    /**
     *
     * @param objResult the current changes to the inputData item
     * @param individualIndex the index of the inputData content item, different from index in case of replicas
     * @param propertyKey the current value's key
     * @param valueToSet the value to set
     * @param obj the item of inputData being searched through
     * @param index the index of the inputData content item
     * @returns the objResult with the values set
     */
    setObjectResult(objResult, individualIndex, propertyKey, valueToSet, obj, index) {
        objResult.content[individualIndex][propertyKey].value = valueToSet;
        objResult.content[individualIndex][propertyKey].interactedWith = true;
        objResult.content[individualIndex][propertyKey].empty = false;
        objResult.content[individualIndex][propertyKey].problem = this.checkRestrictions(
            obj.content[index][propertyKey],
            valueToSet,
            obj.content[index][propertyKey].regexRestriction,
            obj.content[index][propertyKey].validUrl
        );
        return objResult;
    }

    closeWizard = () => {
        const { wizardToggleDisplay } = this.props;
        wizardToggleDisplay();
    };

    doneWizard = () => {
        const { sendYAML, navsObj, notifyError, yamlObject, serviceId, enablerName } = this.props;

        /**
         * Check that all mandatory fields are filled.
         * @param navs navsObj contains information about all unfilled fields in each nav tab.
         * @returns {boolean} true if no mandatory fields are empty
         */
        const presenceIsSufficient = (navs) => {
            let sufficient = true;
            Object.keys(navs).forEach((nav) => {
                Object.keys(navs[nav]).forEach((category) => {
                    if (Array.isArray(navs[nav][category])) {
                        navs[nav][category].forEach((set) => {
                            if (set.length > 0) {
                                sufficient = false;
                            }
                        });
                    }
                });
            });
            return sufficient;
        };
        if (enablerName !== 'Static Onboarding' || !this.props.userCanAutoOnboard) {
            this.closeWizard();
        } else if (presenceIsSufficient(navsObj)) {
            sendYAML(YAML.stringify(yamlObject), serviceId);
        } else {
            notifyError('Fill all mandatory fields first!');
        }
    };

    /**
     * Displays either Next or Save, depending whether the user is at the last stage or not.
     */
    nextSave = () => {
        const { selectedCategory, navsObj, nextWizardCategory, validateInput } = this.props;
        if (selectedCategory < Object.keys(navsObj).length) {
            validateInput(Object.keys(navsObj)[selectedCategory], false);
            nextWizardCategory();
            if (selectedCategory === Object.keys(navsObj).length - 1) {
                const navNamesArr = Object.keys(this.props.navsObj);
                navNamesArr.forEach((navName) => {
                    this.props.validateInput(navName, false);
                });
            }
        } else {
            this.doneWizard();
        }
    };

    /**
     * Convert an uploaded yaml file to JSON and save in redux
     */
    showFile(e) {
        const { updateUploadedYamlTitle, notifyInvalidYamlUpload } = this.props;
        e.preventDefault();
        const reader = new FileReader();
        const filepath = e.target.files[0];
        const filename = filepath.name;
        reader.onload = (event) => {
            const text = event.target.result;
            try {
                const obj = yaml.load(text);
                if (typeof obj !== 'object') {
                    throw Error('File not valid yaml!');
                }
                this.fillInputs(obj);
                updateUploadedYamlTitle(filename);
            } catch {
                if (document.getElementById('yaml-browser')) {
                    document.getElementById('yaml-browser').value = null;
                }
                notifyInvalidYamlUpload('Please make sure the file you are uploading is in valid YAML format!');
            }
        };
        reader.readAsText(e.target.files[0]);
    }

    /**
     * Fills in the field specified by the indentation dependency
     * @param inputData inputData array with all categories (already filled by user)
     * @param indentationDependency the name of the input on which it depends
     * @param indentationDependencyValue the value for the input
     */
    fillIndentationDependency = (inputData, indentationDependency, indentationDependencyValue) => {
        inputData.forEach((category) => {
            const objResult = { ...category };
            if (Array.isArray(category.content)) {
                category.content.forEach((inpt, j) => {
                    Object.keys(inpt).forEach((k) => {
                        if (k === indentationDependency) {
                            objResult.content[j][k].value = indentationDependencyValue;
                        }
                    });
                });
            } else {
                Object.keys(category.content).forEach((k) => {
                    if (k === indentationDependency) {
                        objResult.content[k].value = indentationDependencyValue;
                    }
                });
            }
            this.props.updateWizardData(objResult);
        });
    };

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
                return problem || false;
            } catch {
                inputObject.tooltip = 'The URL has to be valid, example: https://localhost:10014';
                return true;
            }
        }
        return problem;
    }

    /**
     * Adds the replicas of content including for minions when necessary
     * @param obj the item of inputData being searched through
     * @param objResult the current changes to the inputData item
     * @param index the index of the inputData content item
     * @returns updated object
     */
    addNecessaryReplicas = (obj, objResult, index) => {
        if (objResult.content.length <= index) {
            objResult.content.push(JSON.parse(JSON.stringify(obj.content[0])));
        }
        let otherObj;
        if (obj.minions) {
            Object.keys(obj.minions).forEach((minion) => {
                this.props.inputData.forEach((currentObj) => {
                    if (currentObj.text === minion) {
                        otherObj = { ...currentObj };
                    }
                });
                if (otherObj) {
                    if (otherObj.content.length <= index) {
                        otherObj.content.push(JSON.parse(JSON.stringify(otherObj.content[0])));
                    }
                    this.props.updateWizardData(otherObj);
                }
            });
        }
        return objResult;
    };

    /**
     * Check if there are any minions attached and update them if a shared value has been modified
     * @param obj the item of inputData being searched through
     * @param name key of the value that has been changed
     * @param value the new value
     * @param arrIndex index of the set
     */
    propagateToMinions(obj, name, value, arrIndex) {
        const { minions } = obj;
        if (minions) {
            if (Object.values(minions)[0].includes(name)) {
                let category;
                this.props.inputData.forEach((cat) => {
                    if (cat.text === Object.keys(minions)[0]) {
                        category = { ...cat };
                    }
                });
                if (typeof category !== 'undefined') {
                    const arr = [...category.content];
                    if (arr[arrIndex]) {
                        arr[arrIndex] = {
                            ...arr[arrIndex],
                            [name]: {
                                ...category.content[arrIndex][name],
                                value,
                                interactedWith: true,
                                empty: false,
                                problem: this.checkRestrictions(
                                    category.content[arrIndex][name],
                                    value,
                                    category.content[arrIndex][name].regexRestriction,
                                    category.content[arrIndex][name].validUrl
                                ),
                            },
                        };
                        this.props.updateWizardData({ ...category, content: arr });
                    }
                }
            }
        }
    }

    /**
     * Get the new list of calls to propagate to minions
     * @param minionCalls the list of minion calls
     * @param minions the list of minions
     * @param obj the item of inputData being searched through
     * @param name key of the value that has been changed
     * @param value the new value
     * @param index index of the set
     * @returns an appended list of minion calls
     */
    addMinionCall = (minionCalls, minions, obj, name, value, index) => {
        if (minions) {
            return [...minionCalls, { obj, name, value, index }];
        }
        return minionCalls;
    };

    /**
     * Add to the inputData content
     * @param obj the item of inputData being searched through
     * @param objResultIn the current changes to the inputData item
     * @param index the index of the inputData content item
     * @param propertyKey the current value's key
     * @param value the value to be set from the yaml
     * @returns updated object and added minion calls
     */
    addToContent = (obj, objResultIn, index, propertyKey, value) => {
        let objResult = { ...objResultIn };
        let minionCalls = [];
        // If this path exists in the uploaded yaml and its a field that has potential replicas
        if (obj.multiple && obj.multiple === true) {
            value.forEach((individualValue, individualIndex) => {
                objResult = this.addNecessaryReplicas(obj, objResult, individualIndex);
                let valueToSet = null;
                if (obj.noKey && obj.noKey === true && individualValue) {
                    valueToSet = individualValue;
                } else if (obj.arrIndent && individualValue[obj.arrIndent][propertyKey]) {
                    valueToSet = individualValue[obj.arrIndent][propertyKey];
                } else if (individualValue[propertyKey]) {
                    valueToSet = individualValue[propertyKey];
                }
                if (valueToSet != null) {
                    objResult = this.setObjectResult(objResult, individualIndex, propertyKey, valueToSet, obj, index);
                    minionCalls = this.addMinionCall(
                        minionCalls,
                        objResult.minions,
                        obj,
                        propertyKey,
                        valueToSet,
                        individualIndex
                    );
                }
            });
        } else if (value[propertyKey]) {
            objResult = this.setObjectResult(objResult, index, propertyKey, value[propertyKey], obj, index);
            minionCalls = this.addMinionCall(minionCalls, objResult.minions, obj, propertyKey, value[propertyKey], 0);
        }
        return { val1: objResult, val2: minionCalls };
    };

    /**
     * Find each value from the yaml to fill the fields
     * @param obj the item of inputData being searched through
     * @param objResultIn the current changes to the inputData item
     * @param uploadedYaml the yaml object uploaded by the user
     * @returns updated object and added minion calls
     */
    findValue = (obj, objResultIn, uploadedYaml) => {
        let objResult = { ...objResultIn };
        let path = [];
        // Get indentation to mimic hierarchy of yaml
        if (obj.indentation) {
            path = obj.indentation.split('/');
        }
        let value = uploadedYaml;
        let found = true;
        // Set value if it is in an array
        if (obj.inArr) {
            [value] = value.services;
        }
        // Navigate down Yaml hierarchy to find desired value to change to
        if (path.length > 0) {
            path.forEach((indent) => {
                if (found && value[indent]) {
                    value = value[indent];
                } else {
                    found = false;
                }
            });
        }
        if (obj.indentationDependency) {
            this.fillIndentationDependency(this.props.inputData, obj.indentationDependency, Object.keys(value)[0]);
            value = value[Object.keys(value)[0]];
        }
        let minionCalls = [];
        if (found) {
            // Loop through all possible replicas of an input group (e.g. Routes 1 (field1, field2), Routes 2 (field1, field2))
            obj.content.forEach((property, index) => {
                // Loop through each input in an input group replica
                Object.keys(property).forEach((propertyKey) => {
                    const { val1, val2 } = this.addToContent(obj, objResult, index, propertyKey, value);
                    objResult = val1;
                    minionCalls = [...minionCalls, ...val2];
                });
            });
        }
        return { val1: objResult, val2: minionCalls };
    };

    /**
     * Go through the uploaded yaml file and fill the corresponding input fields
     * @param uploadedYaml the yaml object uploaded by the user
     */
    fillInputs(uploadedYaml) {
        let minionCalls = [];
        if (this.props.inputData) {
            // Loop through all input groups (tabs/sections)
            this.props.inputData.forEach((obj) => {
                let objResult = { ...obj };
                if (obj.content) {
                    const { val1, val2 } = this.findValue(obj, objResult, uploadedYaml);
                    objResult = val1;
                    minionCalls = [...minionCalls, ...val2];
                }
                this.props.updateWizardData(objResult);
            });
        }
        // Do all propagations to minions
        minionCalls.forEach((minionCall) => {
            this.propagateToMinions(minionCall.obj, minionCall.name, minionCall.value, minionCall.index);
        });
        // Validate all fields
        const navNamesArr = Object.keys(this.props.navsObj);
        navNamesArr.forEach((navName) => {
            this.props.validateInput(navName, false);
        });
    }

    renderDoneButtonText() {
        if (this.props.enablerName === 'Static Onboarding' && this.props.userCanAutoOnboard) {
            return 'Save';
        }
        return 'Done';
    }

    render() {
        const { wizardIsOpen, enablerName, selectedCategory, navsObj, updateUploadedYamlTitle } = this.props;
        const size = selectedCategory === Object.keys(navsObj).length ? 'large' : 'medium';
        const disable = selectedCategory !== Object.keys(navsObj).length;
        return (
            <div className="dialog">
                <Dialog id="wizard-dialog" open={wizardIsOpen} size={size}>
                    <DialogTitle>Onboard a New API Using {enablerName}</DialogTitle>
                    <DialogContent>
                        <DialogContentText>
                            This wizard will guide you through creating a correct YAML for your application.
                        </DialogContentText>
                        <DialogContentText>
                            Select your YAML configuration file to prefill the fields:
                        </DialogContentText>
                        <div id="yaml-upload-container">
                            <label id="yaml-upload" nesting="true" htmlFor="yaml-browser">
                                <input
                                    id="yaml-browser"
                                    type="file"
                                    onChange={this.showFile}
                                    data-testid="yaml-upload-test"
                                />
                                Choose File
                            </label>
                            {this.props.uploadedYamlTitle ? (
                                <span id="yaml-file-text" data-testid="yaml-file-text-test">
                                    {this.props.uploadedYamlTitle}
                                </span>
                            ) : null}
                        </div>
                        <DialogContentText>Or fill the fields:</DialogContentText>
                        <WizardNavigationContainer />
                    </DialogContent>
                    <DialogActions>
                        <IconButton
                            id="wizard-cancel-button"
                            size="medium"
                            onClick={() => {
                                this.closeWizard();
                                updateUploadedYamlTitle('');
                            }}
                            style={{ borderRadius: '0.1875em' }}
                        >
                            Cancel
                        </IconButton>
                        <IconButton
                            id="wizard-done-button"
                            size="medium"
                            onClick={this.nextSave}
                            disabled={disable}
                            style={{ borderRadius: '0.1875em' }}
                        >
                            {this.renderDoneButtonText()}
                        </IconButton>
                    </DialogActions>
                </Dialog>
            </div>
        );
    }
}
