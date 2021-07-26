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
                <Tabs position="start" label="Categories">
                    {this.loadTabs()}
                </Tabs>
            </div>
        );
    }
}

export default WizardNavigation;
