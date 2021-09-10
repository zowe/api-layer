import React, { Component } from 'react';
import { IconError } from 'mineral-ui-icons';
import Tabs, { Tab } from 'mineral-ui/Tabs';
import { Card, CardBlock, Link } from 'mineral-ui';
import WizardInputsContainer from './WizardInputsContainer';
import YAMLVisualizerContainer from '../YAML/YAMLVisualizerContainer';

class WizardNavigation extends Component {
    constructor(props) {
        super(props);
        this.handleChange = this.handleChange.bind(this);
        this.returnNavs = this.returnNavs.bind(this);
    }

    /**
     * React on navTab click
     * @param event number - index of the tab to be switched to
     */
    handleChange = event => {
        if (typeof event === 'number') {
            const navNamesArr = Object.keys(this.props.navsObj);
            if (this.props.selectedCategory < navNamesArr.length) {
                this.props.validateInput(navNamesArr[this.props.selectedCategory], false);
            }
            if (event === navNamesArr.length) {
                navNamesArr.forEach(navName => {
                    this.props.validateInput(navName, false);
                });
            }
            this.props.changeWizardCategory(event);
        }
    };

    /**
     * Handles grouping of multiple categories under a single navTab
     * @returns {{}} object containing all the categories that should be under the specific tab grouped under a bigger object
     */
    returnNavs() {
        const navs = {};
        let index = 0;
        this.props.inputData.forEach(category => {
            if (!Array.isArray(navs[category.nav])) {
                navs[category.nav] = [];
            }
            if (category.help) {
                navs[category.nav].push(
                    <Card key={`card#${index}`} className="wizardCategoryInfo">
                        <CardBlock>{category.help}</CardBlock>
                        {category.helpUrl ? (
                            <CardBlock>
                                <Link target="_blank" href={category.helpUrl.link}>
                                    {category.helpUrl.title}
                                </Link>
                            </CardBlock>
                        ) : null}
                    </Card>
                );
            }
            navs[category.nav].push(<WizardInputsContainer key={`nav#${index}`} data={category} />);
            index += 1;
        });
        return navs;
    }

    /**
     * Creates a tab for the category/categories
     * @returns {unknown[]} a Tab to be rendered
     */
    loadTabs = () => {
        let index = 0;
        const categories = Object.entries(this.returnNavs()).map(entry => {
            const name = entry[0];
            const categoryArr = entry[1];
            index += 1;
            return (
                <Tab key={index} title={name} icon={this.props.navsObj[name].warn ? <IconError /> : undefined}>
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
