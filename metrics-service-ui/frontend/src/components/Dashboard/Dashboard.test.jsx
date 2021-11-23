/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

/* eslint-disable no-undef */

import React from 'react';
import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import Dashboard from './Dashboard';
import {act} from "react-dom/test-utils";
import {render} from 'react-dom';

describe('>>> Dashboard component tests', () => {


    it('should display the service name ', async () => {

        window.addStreams = jest.fn().mockReturnValue(()=>{});
        const mock = new MockAdapter(axios);
        mock.onGet().reply(200, [{name:"GATEWAY",link:"https://127.0.0.1:10010/turbine.stream?cluster=GATEWAY"}]);

        let container = document.createElement("div");
        document.body.appendChild(container);

        await act(async () => render(<Dashboard/>,container));
        expect(container.textContent).toBe("Metrics ServiceGATEWAY");
    });
});
