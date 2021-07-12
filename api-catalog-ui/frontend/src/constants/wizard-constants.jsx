/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

export const TOGGLE_DISPLAY = 'TOGGLE_DISPLAY';
export const SAVE_FILE = 'SAVE_FILE';
export const data = [
    {
        text: 'Basic info',
        content: {
            serviceId: '',
            title: '',
            description: '',
            baseUrl: '',
        },
    },
    {
        text: 'URL',
        content: {
            homePageRelativeUrl: '',
            statusPageRelativeUrl: '',
            healthCheckRelativeUrl: '',
        },
    },
    {
        text: 'Discovery Service URL',
    },
    {
        text: 'Routes',
    },
    {
        text: 'API info',
    },
    {
        text: 'Catalog',
    },
    {
        text: 'SSL',
    },
];
