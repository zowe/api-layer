import { Component } from 'react';
import { IconButton, InputLabel, Select, Typography, FormControl, MenuItem } from '@material-ui/core';
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
                    <Typography>Compare</Typography>
                    <FormControl className="formField">
                        <InputLabel shrink>{versionData.text}</InputLabel>
                        <Select
                            label="versionSelect1"
                            value={selectedVersion1}
                            onChange={this.handleVersion1Change}
                            sx={selectorStyle}
                        >
                            {versions.map(version => (
                                <MenuItem value={version}>{version}</MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <Typography>with</Typography>
                    <FormControl className="formField">
                        <InputLabel shrink>{versionData.text}</InputLabel>
                        <Select
                            label="versionSelect2"
                            value={selectedVersion2}
                            onChange={this.handleVersion2Change}
                            sx={selectorStyle}
                        >
                            {versions.map(version => (
                                <MenuItem value={version}>{version}</MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <IconButton
                        disabled={selectedVersion1 == null || selectedVersion2 == null}
                        id="diff-button"
                        data-testid="diff-button"
                        onClick={() => {
                            getDiff(serviceId, selectedVersion1.text, selectedVersion2.text);
                        }}
                    >
                        Go
                    </IconButton>
                </div>
                {/* eslint-disable-next-line react/no-danger */}
                <div className="api-diff-content" dangerouslySetInnerHTML={{ __html: diffText }} />
            </div>
        );
    }
}
