import React, { Component } from 'react';
import { Button } from 'mineral-ui';
import { IconChevronRight } from 'mineral-ui-icons';

class WizardNavigation extends Component {
    render() {
        const { nextWizardCategory } = this.props;
        return (
            <div>
                <Button id="next" onClick={nextWizardCategory} iconStart={<IconChevronRight />} />
            </div>
        );
    }
}

export default WizardNavigation;
