import React, { Component } from 'react';
import { Button, Select, Text } from 'mineral-ui';
import './ServiceVersionDiff.css';

export default class ServiceVersionDiff extends Component {
    constructor(props) {
        const { version1, version2 } = props;
        super(props);
        this.state = {
            selectedVersion1: version1 ? { text: version1 } : undefined,
            selectedVersion2: version2 ? { text: version2 } : undefined,
        };

        this.handleVersion1Change = this.handleVersion1Change.bind(this);
        this.handleVersion2Change = this.handleVersion2Change.bind(this);
    }

    handleVersion1Change(version) {
        this.setState({ selectedVersion1: version });
    }

    handleVersion2Change(version) {
        this.setState({ selectedVersion2: version });
    }

    render() {
        const { serviceId, versions, getDiff, diffText } = this.props;
        const { selectedVersion1, selectedVersion2 } = this.state;
        const versionData = versions.map(version => ({ text: version }));
        const selectorStyle = {
            width: '140px',
        };
        return (
            <div className="api-diff-container">
                <div className="api-diff-form">
                    <Text>Compare</Text>
                    <Select
                        data={versionData}
                        name="versionSelect1"
                        selectedItem={selectedVersion1}
                        onChange={this.handleVersion1Change}
                        style={selectorStyle}
                    />
                    <Text>with</Text>
                    <Select
                        data={versionData}
                        name="versionSelect2"
                        selectedItem={selectedVersion2}
                        onChange={this.handleVersion2Change}
                        style={selectorStyle}
                    />
                    <Button
                        data-testid="diff-button"
                        onClick={() => {
                            getDiff(serviceId, selectedVersion1.text, selectedVersion2.text);
                        }}
                    >
                        Go
                    </Button>
                </div>
                <div className="api-diff-content" dangerouslySetInnerHTML={{ __html: diffText }} />
            </div>
        );
    }
}
