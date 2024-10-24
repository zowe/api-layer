/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
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
                question: `When the value is true, the Gateway allows encoded characters to be part of URL requests redirected through the Gateway. The default setting of false is the recommended setting. Change this setting to true only if you expect certain encoded characters in your application's requests`,
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
