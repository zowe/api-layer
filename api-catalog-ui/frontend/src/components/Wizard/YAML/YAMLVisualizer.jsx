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
import * as YAML from 'yaml';
import { Button } from '@material-ui/core';
import FileCopy from '@material-ui/icons/FileCopy';

class YAMLVisualizer extends Component {
    componentDidMount() {
        this.props.createYamlObject(this.props.inputData);
    }

    /**
     * copies YAML to clipboard
     */
    copy = () => {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(YAML.stringify(this.props.yamlObject));
        } else {
            const selection = window.getSelection();
            const el = document.querySelector('.yamlContainer code');
            const range = document.createRange();
            range.selectNodeContents(el);
            selection.removeAllRanges();
            selection.addRange(range);
            document.execCommand('copy');
            selection.removeAllRanges();
        }
    };

    /**
     * Uses yamlObject from props to create YAML text
     * @returns {unknown[]} formatted YAML
     */

    render() {
        return (
            <div className="yamlContainer">
                <div id="copyButtonContainer">
                    <Button onClick={this.copy} size="small" startIcon={<FileCopy />}>
                        Copy
                    </Button>
                </div>
                <code>{YAML.stringify(this.props.yamlObject)}</code>
            </div>
        );
    }
}

export default YAMLVisualizer;
