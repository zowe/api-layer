/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import { wizRegex } from './wizard_regex_restrictions';

export const staticSpecificCategories = [
    {
        text: 'URL for Static',
        content: {
            instanceBaseUrls: {
                value: '',
                question: 'The base URL of the instance (the consistent part of the web address):',
                tooltip: 'Example: http://localhost:8080',
            },
        },
        multiple: false,
        noKey: true,
    },
    {
        text: 'Routes for Static & Node',
        content: {
            gatewayUrl: {
                value: '',
                question: 'Expose the Service API on Gateway under context path:',
                tooltip: 'Format: /api/vX, Example: /api/v1',
                regexRestriction: [wizRegex.gatewayUrl],
            },
            serviceRelativeUrl: {
                value: '',
                question: 'Service API common context path:',
                tooltip: 'Example: /sampleservice/api/v1',
            },
        },
        help: 'For service: <service>/allOfMyEndpointsAreHere/** exposed on Gateway under <gateway>/<serviceid>/api/v1/**\nFill in:\ngatewayUrl: /api/v1\nserviceUrl: /allOfMyEndpointsAreHere',
        multiple: true,
    },
];
