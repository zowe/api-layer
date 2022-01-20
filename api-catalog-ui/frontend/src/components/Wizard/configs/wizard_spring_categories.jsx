/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
// eslint-disable-next-line import/prefer-default-export
export const springSpecificCategories = [
    {
        text: 'Enable',
        content: {
            enabled: {
                value: false,
                question: 'Service should automatically register with API ML discovery service',
            },
            enableUrlEncodedCharacters: {
                value: false,
                question: 'Service requests the API ML GW to receive encoded characters in the URL',
            },
        },
    },
    {
        text: 'Spring',
        content: {
            name: {
                value: '',
                question: 'This parameter has to be the same as the service ID you are going to provide',
            },
        },
    },
];
