/* eslint-disable no-undef */

import { getVisibleTiles } from './selectors';

const tiles = [
    {
        version: '1.0.0',
        id: 'suspendisse',
        title: 'Title Lorem ipsum hac conubia ante mi aptent venenatis.',
        status: 'UP',
        description:
            'DescriptionCommon Lorem ipsum faucibus molestie phasellus platea praesent est blandit pellentesque',
        services: [],
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
        services: [],
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
        services: [],
        totalServices: 0,
        activeServices: 0,
        lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
        createdTimestamp: '2018-08-22T08:31:22.948+0000',
    },
];

describe('>>> Selector tests', () => {
    it('should return 3 tiles if common title word - Title is used', () => {
        const result = getVisibleTiles(tiles, 'Title');
        expect(result.length).toEqual(3);
    });
    it('should return 3 tiles if common title word - Title is used - mixed case', () => {
        const result = getVisibleTiles(tiles, 'tiTlE');
        expect(result.length).toEqual(3);
    });
    it('should return 0 tiles if missing title word is used', () => {
        const result = getVisibleTiles(tiles, 'flashy');
        expect(result.length).toEqual(0);
    });
    it('should return 3 tiles if common description word - Description is used', () => {
        const result = getVisibleTiles(tiles, 'Description');
        expect(result.length).toEqual(3);
    });
    it('should return 3 tiles if common description word - Description is used - mixed case', () => {
        const result = getVisibleTiles(tiles, 'DeSCriPtiOn');
        expect(result.length).toEqual(3);
    });
    it('should return 0 tiles if missing description word is used', () => {
        const result = getVisibleTiles(tiles, 'flashy');
        expect(result.length).toEqual(0);
    });
    it('should return 3 tiles if criteria is empty', () => {
        let result = getVisibleTiles(tiles, undefined);
        expect(result.length).toEqual(3);
        result = getVisibleTiles(tiles, null);
        expect(result.length).toEqual(3);
        result = getVisibleTiles(tiles, '');
        expect(result.length).toEqual(3);
    });
    it('should return 0 tiles if tiles is empty', () => {
        let result = getVisibleTiles([], 'asdsd');
        expect(result.length).toEqual(0);
        result = getVisibleTiles([], 'a');
        expect(result.length).toEqual(0);
        result = getVisibleTiles([], '');
        expect(result.length).toEqual(0);
    });
});
