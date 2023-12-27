/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import { getFilteredServices, sortServices } from './selectors';

const tiles = [
    {
        version: '1.0.0',
        id: 'suspendisse',
        title: 'Title Lorem ipsum hac conubia ante mi aptent venenatis.',
        status: 'UP',
        description:
            'DescriptionCommon Lorem ipsum faucibus molestie phasellus platea praesent est blandit pellentesque',
        services: [
            {
                version: '1.0.0',
                id: 'suspendisse',
                title: 'Title Lorem ipsum hac conubia ante mi aptent venenatis.',
                status: 'UP',
                description:
                    'DescriptionCommon Lorem ipsum faucibus molestie phasellus platea praesent est blandit pellentesque',
            },
        ],
        totalServices: 0,
        activeServices: 0,
        lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
        createdTimestamp: '2018-08-22T08:31:22.948+0000',
    },
    {
        version: '1.0.0',
        id: 'ullamcorper',
        title: 'TitleLorem ipsum hac conubia ante mi aptent venenatis.',
        status: 'UP',
        description: 'Description Aenean facilisis venenatis quam aenean dictumst sapien mollis ultricies erat semper.',
        services: [
            {
                version: '1.0.0',
                id: 'ullamcorper',
                title: 'TitleLorem ipsum hac conubia ante mi aptent venenatis.',
                status: 'UP',
                description:
                    'Description Aenean facilisis venenatis quam aenean dictumst sapien mollis ultricies erat semper.',
            },
            {
                version: '1.0.0',
                id: 'ullamcorper2',
                title: 'TitleLorem ipsum hac conubia ante mi aptent venenatis.',
                status: 'UP',
                description:
                    'Description Aenean facilisis venenatis quam aenean dictumst sapien mollis ultricies erat semper.',
            },
        ],
        totalServices: 0,
        activeServices: 0,
        lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
        createdTimestamp: '2018-08-22T08:31:22.948+0000',
    },
    {
        version: '1.0.0',
        id: 'habitasse',
        title: 'Title Lorem ipsum hac conubia ante mi aptent venenatis.',
        status: 'UP',
        description: 'Description euismod, morbi potenti condimentum suscipit sapien.',
        services: [
            {
                version: '1.0.0',
                id: 'habitasse',
                title: 'TitleLorem ipsum hac conubia ante mi aptent venenatis.',
                status: 'UP',
                description:
                    'Description Aenean facilisis venenatis quam aenean dictumst sapien mollis ultricies erat semper.',
            },
        ],
        totalServices: 0,
        activeServices: 0,
        lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
        createdTimestamp: '2018-08-22T08:31:22.948+0000',
    },
];

describe('>>> Selector tests', () => {
    it('should return 3 tiles if common title word - Title is used', () => {
        const result = getFilteredServices(tiles, 'Title');
        expect(result.length).toEqual(3);
    });
    it('should return 3 tiles if common title word - Title is used - mixed case', () => {
        const result = getFilteredServices(tiles, 'tiTlE');
        expect(result.length).toEqual(3);
    });
    it('should return 0 tiles if missing title word is used', () => {
        const result = getFilteredServices(tiles, 'flashy');
        expect(result.length).toEqual(0);
    });
    it('should return 0 tiles if missing description word is used', () => {
        const result = getFilteredServices(tiles, 'flashy');
        expect(result.length).toEqual(0);
    });
    it('should return 3 tiles if criteria is empty', () => {
        let result = getFilteredServices(tiles, undefined);
        expect(result.length).toEqual(3);
        result = getFilteredServices(tiles, null);
        expect(result.length).toEqual(3);
        result = getFilteredServices(tiles, '');
        expect(result.length).toEqual(3);
    });
    it('should return 0 tiles if tiles is empty', () => {
        let result = getFilteredServices([], 'asdsd');
        expect(result.length).toEqual(0);
        result = getFilteredServices([], 'a');
        expect(result.length).toEqual(0);
        result = getFilteredServices([], '');
        expect(result.length).toEqual(0);
    });
    it('should sort services alphabetically by title', () => {
        const unsortedServices = [
            { title: 'B Service' },
            { title: 'D Service' },
            { title: 'A Service' },
            { title: 'C Service' },
        ];

        const unsortedTiles = [
            { services: [unsortedServices[2], unsortedServices[1]] },
            { services: [unsortedServices[0], unsortedServices[3]] },
        ];

        const sortedServices = sortServices(unsortedTiles);

        const expectedSortedServices = [
            unsortedServices[2], // A Service
            unsortedServices[0], // B Service
            unsortedServices[3], // C Service
            unsortedServices[1], // D Service
        ];

        expect(sortedServices).toEqual(expectedSortedServices);
    });

    it('should handle empty services', () => {
        const unsortedTiles = [{ services: [] }, { services: [] }];

        const sortedServices = sortServices(unsortedTiles);

        expect(sortedServices).toEqual([]);
    });
});
