import React, { Component } from 'react';
import Tabs, { Tab } from 'mineral-ui/Tabs';
import WizardInputs from './WizardInputs';

class WizardNavigation extends Component {
    loadTabs() {
        return this.props.inputData.map(category => (
            <Tab title={category.text}>
                <WizardInputs data={category} />
            </Tab>
        ));
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
