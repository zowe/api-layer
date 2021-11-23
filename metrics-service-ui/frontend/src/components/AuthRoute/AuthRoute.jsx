/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import React, { useEffect, useState } from 'react';
import { Redirect, Route } from 'react-router-dom';

import Spinner from '../Spinner/Spinner';
import { userService } from '../../services';

export default function AuthRoute(props) {
    // eslint-disable-next-line
    console.log('AUTH ROUTE AUTHENTICATED: ' + props.authenticated);
    // TODO do away with state, just check is authenticated with api call
    // e.g. https://stackoverflow.com/questions/46162278/authenticate-async-with-react-router-v4
    // have this in AuthRoute component, no middleware, no state
    // can re-check every XX seconds

    const [isLoading, setIsLoading] = useState(true);
    const [isAuthenticated, setIsAuthenticated] = useState(false);

    useEffect(() => {
        userService
            .checkAuthentication()
            .then(() => {
                // eslint-disable-next-line
                console.log('auth check passed in auth route');
                setIsAuthenticated(true);
                setIsLoading(false);
            })
            .catch((error) => {
                // eslint-disable-next-line
                console.log('auth check failed in auth route');
                setIsAuthenticated(false);
                setIsLoading(false);
            });
    });

    if (isLoading) {
        return <Spinner isLoading />;
    }

    if (!isAuthenticated) {
        return <Redirect replace to="/login" />;
    }

    return <Route {...props} />;
}
