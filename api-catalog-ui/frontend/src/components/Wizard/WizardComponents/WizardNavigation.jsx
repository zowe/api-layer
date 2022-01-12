import { Component } from 'react';
import { Tab, Tabs, Card, CardContent, Link, Box } from '@material-ui/core';
import { IconDanger } from 'mineral-ui-icons';
import WizardInputsContainer from './WizardInputsContainer';
import YAMLVisualizerContainer from '../YAML/YAMLVisualizerContainer';

class WizardNavigation extends Component {

    constructor(props) {
        super(props);
        this.handleChange = this.handleChange.bind(this);
        this.returnNavs = this.returnNavs.bind(this);
        this.state = {value: 0};
    }

    /**
     * React on navTab click
     * @param event number - index of the tab to be switched to
     */
    handleChange = (event, value) => {
        if (typeof value === 'number') {
            this.setState({value})
            const navNamesArr = Object.keys(this.props.navsObj);
            if (this.props.selectedCategory < navNamesArr.length) {
                this.props.validateInput(navNamesArr[this.props.selectedCategory], false);
            }
            if (value === navNamesArr.length) {
                this.props.assertAuthorization();
                navNamesArr.forEach(navName => {
                    this.props.validateInput(navName, false);
                });
            }
            this.props.changeWizardCategory(value);
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
                        <CardContent>{category.help}</CardContent>
                        {category.helpUrl ? (
                            <CardContent>
                                <Link target="_blank" href={category.helpUrl.link}>
                                    {category.helpUrl.title}
                                </Link>
                            </CardContent>
                        ) : null}
                    </Card>
                );
            }
            navs[category.nav].push(<WizardInputsContainer key={`nav#${index}`} data={category} />);
            index += 1;
        });
        return navs;
    }
    a11yProps = (index) => {
        return {
            id: `vertical-tab-${index}`,
            'aria-controls': `vertical-tabpanel-${index}`,
        };
    }

    /**
     * Creates a tab for the category/categories
     * @returns {unknown[]} a Tab to be rendered
     */
    loadTabs = () => {
        let index = -1;
        const categories = Object.entries(this.returnNavs()).map(entry => {
            const name = entry[0];
            const categoryArr = entry[1];
            index += 1;
            const done = !this.props.navsObj[name].silent && !this.props.navsObj[name].warn;
            return (
                <Tab
                    className={done ? 'readyTab' : undefined}
                    label={name}
                    icon={this.props.navsObj[name].warn ? <IconDanger style={{ color: 'red' }} /> : undefined}
                    {...this.a11yProps(index)}
                />
            );
        });
        const yamlTab = [];

        yamlTab.push(
            <Tab label="YAML result" />
        );
        return categories.concat(yamlTab);
    };

    loadWizard = () => {
        let index = -1;
        const categories = Object.entries(this.returnNavs()).map(entry => {
            const categoryArr = entry[1];
            index += 1;
            return (
                <div
                    role="tabpanel"
                    hidden={this.state.value !== index}
                    id={`vertical-tabpanel-${index}`}
                    aria-labelledby={`vertical-tab-${index}`}
                >
                    {this.state.value === index && (
                        <Box sx={{ width: 400 }}>
                            {categoryArr}
                        </Box>
                    )}

                </div>
            );
        });
        const yamlTab = [];
        index += 1;
        yamlTab.push(
            <div
                role="tabpanel"
                hidden={this.state.value !== index}
                id={`vertical-tabpanel-${index}`}
                aria-labelledby={`vertical-tab-${index}`}
            >
                {this.state.value === index && (
                    <Box sx={{ width: 400, wordWrap: 'break-word' }}>
                        <YAMLVisualizerContainer />
                    </Box>
                )}

            </div>
        );
        return categories.concat(yamlTab);
    };

    render() {

        return (
            <Box
                sx={{ flexGrow: 1, bgcolor: 'background.paper', display: 'flex', height: 500 }}
            >
                <Tabs
                    id="wizard-navigation"
                    position="start"
                    value={this.state.value}
                    onChange={this.handleChange}
                    label="Categories"
                    orientation="vertical"
                    variant="scrollable"
                    sx={{ borderRight: 1, borderColor: 'divider' }}
                >
                    {this.loadTabs()}
                </Tabs>
                {this.loadWizard()}
            </Box>
        );
    }
}

export default WizardNavigation;
