/* eslint-disable no-undef */
import * as React from 'react';
import { shallow } from 'enzyme';
import Tile from './Tile';
import DetailPage from '../DetailPage/DetailPage';

const match = {
    params: {
        tileID: 'apicatalog',
    },
};

const sampleTile = {
    version: '1.0.0',
    id: 'apicatalog',
    title: 'API Mediation Layer API',
    status: 'UP',
    description: 'lkajsdlkjaldskj',
    services: [
        {
            serviceId: 'apicatalog',
            title: 'API Catalog',
            description:
                'MFaaS Microservice to locate and display API documentation for MFaaS discovered microservices',
            status: 'UP',
            secured: false,
            homePageUrl: '/ui/v1/apicatalog',
        },
    ],
    totalServices: 1,
    activeServices: 1,
    lastUpdatedTimestamp: '2018-08-22T08:32:03.110+0000',
    createdTimestamp: '2018-08-22T08:31:22.948+0000',
};

const resetSampleTile = () => {
    sampleTile.status = 'UP';
    sampleTile.totalServices = 1;
};

describe('>>> Tile component tests', () => {
    beforeEach(() => {
        resetSampleTile();
    });

    it('should display API Mediation Layer API tile with correct title', () => {
        const instance = shallow(<Tile tile={sampleTile} />);
        expect(instance.find('API Mediation Layer API')).not.toBeNull();
    });

    it('method getTileStatus() should return correct values', () => {
        resetSampleTile();
        const wrapper = shallow(<Tile tile={sampleTile} />);
        const instance = wrapper.instance();
        expect(instance.getTileStatus(sampleTile)).toBe('success');
        sampleTile.status = 'DOWN';
        expect(instance.getTileStatus(sampleTile)).toBe('danger');
        sampleTile.totalServices = 2;
        sampleTile.status = 'UP';
        expect(instance.getTileStatus(sampleTile)).toBe('warning');
        expect(instance.getTileStatus()).toBe('Status unknown');
        sampleTile.status = 'UNKNOWN';
        expect(instance.getTileStatus(sampleTile)).toBe('Status unknown');
        expect(instance.getTileStatus()).toBe('Status unknown');
    });

    it('method getTileStatusText() should return correct values', () => {
        resetSampleTile();
        const wrapper = shallow(<Tile tile={sampleTile} />);
        const instance = wrapper.instance();
        expect(instance.getTileStatusText(sampleTile)).toBe('All services are running');
        sampleTile.totalServices = 2;
        expect(instance.getTileStatusText(sampleTile)).toBe('1 of 2 services are running');
        resetSampleTile();
        sampleTile.status = 'DOWN';
        expect(instance.getTileStatusText(sampleTile)).toBe('No services are running');
        resetSampleTile();
        sampleTile.status = 'WARNING';
        sampleTile.totalServices = 2;
        expect(instance.getTileStatusText(sampleTile)).toBe('1 of 2 services are running');
        resetSampleTile();
        sampleTile.status = 'UNKNOWN';
        expect(instance.getTileStatusText(sampleTile)).toBe('Status unknown');
        expect(instance.getTileStatusText()).toBe('Status unknown');
    });

    it('should handle tile click', () => {
        const historyMock = { push: jest.fn() };
        const wrapper = shallow(<Tile tile={sampleTile} history={historyMock} match={match} />);
        wrapper.find('Card').simulate('click');
        expect(historyMock.push.mock.calls[0]).toEqual([`/tile/${sampleTile.id}`]);
    });

    it('should shorten description if too long', () => {
        const description =
            'Yourself required no at thoughts delicate landlord it be. Branched dashwood do is whatever it. Farther be chapter at visited married in it pressed. By distrusts procuring be oh frankness existence believing instantly if. Doubtful on an juvenile as of servants insisted. Judge why maids led sir whose guest drift her point. Him comparison especially friendship was who sufficient attachment favourable how. Luckily but minutes ask picture man perhaps are inhabit. How her good all sang more why. ';
        const expected =
            'Yourself required no at thoughts delicate landlord it be. Branched dashwood do is whatever it. Farther be chapter at visited married in it pressed. By distrusts procuring be oh frankness existence believing instantly if. Doubtf...';
        const wrapper = shallow(<Tile tile={sampleTile} />);
        const instance = wrapper.instance();
        expect(instance.shortenDescription(description)).toEqual(expected);
        expect(instance.shortenDescription(description).length).toEqual(230);
    });
});
