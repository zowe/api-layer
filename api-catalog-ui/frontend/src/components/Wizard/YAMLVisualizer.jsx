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

    render() {
        return (
            <div className="yamlContainer">
                <div id="copyButtonContainer">
                    <Button onClick={this.copy} size="small" iconStart={<IconContentCopy />}>
                        Copy
                    </Button>
                </div>
                <code>{YAML.stringify(this.props.yamlObject)}</code>
            </div>
        );
    }
}

export default YAMLVisualizer;
