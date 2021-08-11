import React, { Component } from 'react';
import * as YAML from 'yaml';
import { Button } from 'mineral-ui';
import { IconContentCopy } from 'mineral-ui-icons';

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
    renderYaml() {
        const string = YAML.stringify(this.props.yamlObject);
        let index = 0;
        return string.split('\n').map(part => {
            index += 1;
            const arr = part.split(':');
            if (arr.length === 1) {
                return (
                    <div key={index}>
                        <span className="yamlKey">{}</span>
                        {arr[0]}
                    </div>
                );
            }
            if (arr[0].length > 1) {
                let value = arr[1];
                arr.slice(2).forEach(el => {
                    value += `:${el}`;
                });
                return (
                    <div key={index}>
                        <span className="yamlKey">{arr[0]}</span>:{value}
                    </div>
                );
            }
            return null;
        });
    }

    render() {
        return (
            <div className="yamlContainer">
                <div id="copyButtonContainer">
                    <Button onClick={this.copy} size="small" iconStart={<IconContentCopy />}>
                        Copy
                    </Button>
                </div>
                <code>{this.renderYaml()}</code>
            </div>
        );
    }
}

export default YAMLVisualizer;
