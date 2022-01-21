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
    const [isLoading, setIsLoading] = useState(true);
    const [isAuthenticated, setIsAuthenticated] = useState(false);

    useEffect(() => {
        userService
            .checkAuthentication()
            .then(() => {
                setIsAuthenticated(true);
                setIsLoading(false);
            })
            .catch((error) => {
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
