/* eslint-disable react/destructuring-assignment */
import { Component } from 'react';
import Text from 'mineral-ui/Text';
import '../BigShield/BigShield.css';

export default class Shield extends Component {
    constructor(props) {
        super(props);
        this.state = {
            error: false,
            // eslint-disable-next-line react/no-unused-state
            info: null,
        };
    }

    componentDidCatch(error, info) {
        this.setState({
            error,
            // eslint-disable-next-line react/no-unused-state
            info,
        });
    }

    render() {
        const { title } = this.props;
        if (this.state.error) {
            return (
                <div style={{ width: '40%' }}>
                    <h4 style={{ color: '#de1b1b' }}>{title}</h4>
                    <input id="collapsible" className="toggle" type="checkbox" />
                    {/* eslint-disable-next-line jsx-a11y/label-has-for */}
                    <label htmlFor="collapsible" className="lbl-toggle">
                        Display the error stack
                    </label>
                    <div className="collapsible-content">
                        <div className="content-inner">
                            <Text element="h5">
                                <pre style={{ textAlign: 'left' }}>
                                    <code>{this.state.error.stack}</code>
                                </pre>
                            </Text>
                        </div>
                    </div>
                </div>
            );
        }
        return this.props.children;
    }
}
