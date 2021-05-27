import React from 'react';
import { Redirect, Route } from 'react-router-dom';

const AuthRoute = (props) => {
    const { authenticated } = props;
    if (!authenticated) return <Redirect replace to="/login" />;

    return <Route {...props} />;
};

export default AuthRoute;
