import React from 'react';
import { connect } from 'react-redux';
import { Redirect, Route } from 'react-router-dom';

const AuthRoute = (props) => {
    const { authenticated } = props;
    if (!authenticated) return <Redirect replace to="/login" />;

    return <Route {...props} />;
};

const mapStateToProps = (state) => {
    const authenticated = !!state.authenticationReducer.sessionOn;
    return { authenticated };
};

export default connect(mapStateToProps)(AuthRoute);
