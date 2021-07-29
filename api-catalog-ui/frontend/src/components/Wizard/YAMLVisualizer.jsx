import React, { Component } from 'react';
import * as YAML from 'yaml';

class YAMLVisualizer extends Component {
    componentDidMount() {
        this.props.createYamlObject(this.props.inputData);
    }

    render() {
        return (
            <div className="yamlContainer">
                <code>{YAML.stringify(this.props.yamlObject)}</code>
            </div>
        );
    }
}

export default YAMLVisualizer;
