import React, { Component } from 'react';
import Tabs, { Tab } from 'mineral-ui/Tabs';
import WizardInputs from './WizardInputs';

class WizardNavigation extends Component {
    loadTabs() {
        let index = 0;
        return this.props.inputData.map(category => {
            index += 1;
            return (
                <Tab key={index} title={category.text}>
                    <WizardInputs data={category} />
                </Tab>
            );
        });
    }

    render() {
        return (
            <div>
                <Tabs
                    id="wizard-navigation"
                    position="start"
                    selectedTabIndex={this.props.selectedCategory}
                    onChange={this.props.changeWizardCategory}
                    label="Categories"
                >
                    {this.loadTabs()}
                </Tabs>
            </div>
        );
    }
}

export default WizardNavigation;
