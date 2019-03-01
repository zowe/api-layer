import React, {Component} from 'react';
import './Spinner.css';

export default class Spinner extends Component {
    render() {
        const {isLoading, css} = this.props;
        const divStyle = {
            display: isLoading === true ? 'block' : 'none',
            ...css,
        };
        return (
            <div id="spinner" className="lds-ring" style={divStyle}>
                <div/>
                <div/>
                <div/>
                <div/>
            </div>
        );
    }
}
