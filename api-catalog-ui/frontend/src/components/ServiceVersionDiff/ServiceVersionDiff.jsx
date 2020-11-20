import React, {Component} from 'react';
import Select from 'mineral-ui/Select';
import Button from 'mineral-ui/Button';
import Text from 'mineral-ui/Text';
import './ServiceVersionDiff.css';

export default class ServiceVersionDiff extends Component {
    constructor(props) {
        super(props);
        this.state = {
            selectedVersion1: undefined,
            selectedVersion2: undefined,
        }

        this.handleVersion1Change = this.handleVersion1Change.bind(this);
        this.handleVersion2Change = this.handleVersion2Change.bind(this);
    }

    handleVersion1Change(version) {
        this.setState({selectedVersion1: version});
    }

    handleVersion2Change(version) {
        this.setState({selectedVersion2: version});
    }

    render() {
        const { serviceId, versions, getDiff, diffText } = this.props;
        const { selectedVersion1, selectedVersion2 } = this.state;
        const versionData = versions.map(version => {
            return {text: version}
        })
        const selectorStyle = {
            width : '140px'
        }
        return(
            <div style={{margin: 'auto'}}>
                <div style={{display: 'flex', width: 'fit-content', margin: 'auto'}}>
                    <Text style={{marginTop: 'auto', paddingRight: '5px', paddingLeft: '5px'}} >Compare</Text>
                    <Select 
                        data={versionData} 
                        name="versionSelect1" 
                        selectedItem={selectedVersion1} 
                        onChange={this.handleVersion1Change} 
                        style={selectorStyle} />
                    <Text style={{marginTop: 'auto', paddingRight: '5px', paddingLeft: '5px'}} >with</Text>
                    <Select 
                        data={versionData} 
                        name="versionSelect2" 
                        selectedItem={selectedVersion2} 
                        onChange={this.handleVersion2Change}
                        style={selectorStyle} />
                    <Button onClick={() => { getDiff(serviceId, selectedVersion1.text, selectedVersion2.text) }}>Compare</Button>
                </div>
                <div dangerouslySetInnerHTML={{__html: diffText}} />
            </div>
        )
    }
}