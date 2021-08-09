import React, { Component } from 'react';
import * as YAML from 'yaml';
import { Button } from 'mineral-ui';
import { IconContentCopy } from 'mineral-ui-icons';

class YAMLVisualizer extends Component {
    componentDidMount() {
        this.props.createYamlObject(this.props.inputData);
    }

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
                return (
                    <div key={index}>
                        <span className="yamlKey">{arr[0]}</span>:{arr[1]}
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
