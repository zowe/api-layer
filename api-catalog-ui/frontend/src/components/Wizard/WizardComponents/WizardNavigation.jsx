import React, { Component } from 'react';
import Tabs, { Tab } from 'mineral-ui/Tabs';
import WizardInputsContainer from './WizardInputsContainer';
import YAMLVisualizerContainer from '../YAML/YAMLVisualizerContainer';

class WizardNavigation extends Component {
    constructor(props) {
        super(props);
        this.handleChange = this.handleChange.bind(this);
    }

    handleChange = event => {
        if (typeof event === 'number') {
            this.props.changeWizardCategory(event);
        }
    };
    loadTabs = () => {
        let index = 0;
        const categories = this.props.inputData.map(category => {
            index += 1;
            return (
                <Tab key={index} title={category.text}>
                    <WizardInputsContainer data={category} />
                </Tab>
            );
        });
        const yamlTab = [];
        yamlTab.push(
            <Tab key={0} title="YAML result">
                <YAMLVisualizerContainer />
            </Tab>
        );
        return categories.concat(yamlTab);
    };

    render() {
        return (
            <div>
                <Tabs
                    id="wizard-navigation"
                    position="start"
                    selectedTabIndex={this.props.selectedCategory}
                    onChange={this.handleChange}
                    label="Categories"
                >
                    {this.loadTabs()}
                </Tabs>
            </div>
        );
    }
}

export default WizardNavigation;
