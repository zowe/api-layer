import React, { Component } from 'react';
import Tabs, { Tab } from 'mineral-ui/Tabs';
import WizardInputsContainer from './WizardInputsContainer';
import YAMLVisualizerContainer from '../YAML/YAMLVisualizerContainer';

class WizardNavigation extends Component {
    constructor(props) {
        super(props);
        this.handleChange = this.handleChange.bind(this);
        this.returnNavs = this.returnNavs.bind(this);
    }

    handleChange = event => {
        if (typeof event === 'number') {
            this.props.changeWizardCategory(event);
        }
    };

    returnNavs() {
        const navs = {};
        let index = 0;
        this.props.inputData.forEach(category => {
            if (!Array.isArray(navs[category.nav])) {
                navs[category.nav] = [];
            }
            navs[category.nav].push(<WizardInputsContainer key={`nav#${index}`} data={category} />);
            index += 1;
        });
        return navs;
    }

    loadTabs = () => {
        let index = 0;
        const categories = Object.entries(this.returnNavs()).map(entry => {
            const name = entry[0];
            const categoryArr = entry[1];
            index += 1;
            return (
                <Tab key={index} title={name}>
                    {categoryArr}
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
