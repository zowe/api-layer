import sinon from 'sinon';
import * as chai from 'chai';
import sinonChai from 'sinon-chai';
import AwsMetadata from '../src/AwsMetadata.js';

const expect = chai.expect;

chai.use(sinonChai);

describe('AWS Metadata client', () => {
  describe('fetchMetadata()', () => {
    let client;
    beforeEach(() => {
      client = new AwsMetadata({ host: '127.0.0.1:8888' });
    });

    afterEach(() => {
      global.request.get.restore();
    });

    it('should call metadata URIs', (done) => {
      const requestStub = sinon.stub(global, 'fetch');

      const mock = (url, body) => {
        requestStub.withArgs(url).returns(Promise.resolve({
          error: () => null,
          statusCode: 200,
          body: () => body,
        }));
      };

      mock('http://127.0.0.1:8888/latest/meta-data/ami-id', 'ami-123');
      mock('http://127.0.0.1:8888/latest/meta-data/instance-id', 'i123');
      mock('http://127.0.0.1:8888/latest/meta-data/instance-type', 'medium');
      mock('http://127.0.0.1:8888/latest/meta-data/local-ipv4', '1.1.1.1');
      mock('http://127.0.0.1:8888/latest/meta-data/local-hostname', 'ip-127-0-0-1');
      mock('http://127.0.0.1:8888/latest/meta-data/placement/availability-zone', 'fake-1');
      mock('http://127.0.0.1:8888/latest/meta-data/public-hostname', 'ec2-127-0-0-1');
      mock('http://127.0.0.1:8888/latest/meta-data/public-ipv4', '2.2.2.2');
      mock('http://127.0.0.1:8888/latest/meta-data/mac', 'AB:CD:EF:GH:IJ');
      mock('http://127.0.0.1:8888/latest/dynamic/instance-identity/document', '{"accountId":"123456"}');
      mock('http://127.0.0.1:8888/latest/meta-data/network/interfaces/macs/AB:CD:EF:GH:IJ/vpc-id', 'vpc123');

      const expected = {
        accountId: '123456',
        'ami-id': 'ami-123',
        'availability-zone': 'fake-1',
        'instance-id': 'i123',
        'instance-type': 'medium',
        'local-hostname': 'ip-127-0-0-1',
        'local-ipv4': '1.1.1.1',
        mac: 'AB:CD:EF:GH:IJ',
        'public-hostname': 'ec2-127-0-0-1',
        'public-ipv4': '2.2.2.2',
        'vpc-id': 'vpc123',
      };

      client.fetchMetadata(data => {
        try {
          expect(data).to.deep.equal(expected);
          done();
        } catch (e) {
          done(e);
        }
      });
    });

    it('should call metadata URIs and filter out null and undefined values', () => {
      const requestStub = sinon.stub(global.request, 'get');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/ami-id',
      }).yields(null, { statusCode: 200 }, 'ami-123');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/instance-id',
      }).yields(null, { statusCode: 200 }, 'i123');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/instance-type',
      }).yields(null, { statusCode: 200 }, 'medium');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/local-ipv4',
      }).yields(null, { statusCode: 200 }, '1.1.1.1');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/local-hostname',
      }).yields(null, { statusCode: 200 }, 'ip-127-0-0-1');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/placement/availability-zone',
      }).yields(null, { statusCode: 200 }, 'fake-1');

      let undef;
      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/public-hostname',
      }).yields(null, { statusCode: 200 }, undef);

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/public-ipv4',
      }).yields(null, { statusCode: 200 }, null);

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/mac',
      }).yields(null, { statusCode: 200 }, 'AB:CD:EF:GH:IJ');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/dynamic/instance-identity/document',
      }).yields(null, { statusCode: 200 }, '{"accountId":"123456"}');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/network/interfaces/macs/AB:CD:EF:GH:IJ/vpc-id',
      }).yields(null, { statusCode: 200 }, 'vpc123');

      const fetchCb = sinon.spy();
      client.fetchMetadata(fetchCb);

      expect(global.request.get).to.have.been.callCount(11);
      expect(fetchCb).to.have.been.calledWithMatch({
        accountId: '123456',
        'ami-id': 'ami-123',
        'availability-zone': 'fake-1',
        'instance-id': 'i123',
        'instance-type': 'medium',
        'local-hostname': 'ip-127-0-0-1',
        'local-ipv4': '1.1.1.1',
        mac: 'AB:CD:EF:GH:IJ',
        'vpc-id': 'vpc123',
      });
      expect(fetchCb.firstCall.args[0]).to.have.all.keys(['ami-id',
        'instance-id',
        'instance-type',
        'local-ipv4',
        'local-hostname',
        'availability-zone',
        'mac',
        'accountId',
        'vpc-id']);
    });

    it('should call metadata URIs and filter out errored values', () => {
      const requestStub = sinon.stub(global.request, 'get');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/ami-id',
      }).yields(null, { statusCode: 200 }, 'ami-123');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/instance-id',
      }).yields(null, { statusCode: 200 }, 'i123');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/instance-type',
      }).yields(null, { statusCode: 200 }, 'medium');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/local-ipv4',
      }).yields(null, { statusCode: 200 }, '1.1.1.1');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/local-hostname',
      }).yields(null, { statusCode: 200 }, 'ip-127-0-0-1');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/placement/availability-zone',
      }).yields(null, { statusCode: 200 }, 'fake-1');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/public-hostname',
      }).yields(new Error('fail'));

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/public-ipv4',
      }).yields(new Error('fail'));

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/mac',
      }).yields(null, { statusCode: 200 }, 'AB:CD:EF:GH:IJ');

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/dynamic/instance-identity/document',
      }).yields(new Error('fail'));

      requestStub.withArgs({
        url: 'http://127.0.0.1:8888/latest/meta-data/network/interfaces/macs/AB:CD:EF:GH:IJ/vpc-id',
      }).yields(null, { statusCode: 200 }, 'vpc123');

      const fetchCb = sinon.spy();
      client.fetchMetadata(fetchCb);

      expect(global.request.get).to.have.been.callCount(11);
      expect(fetchCb).to.have.been.calledWithMatch({
        'ami-id': 'ami-123',
        'availability-zone': 'fake-1',
        'instance-id': 'i123',
        'instance-type': 'medium',
        'local-hostname': 'ip-127-0-0-1',
        'local-ipv4': '1.1.1.1',
        mac: 'AB:CD:EF:GH:IJ',
        'vpc-id': 'vpc123',
      });
      expect(fetchCb.firstCall.args[0]).to.have.all.keys(['ami-id',
        'instance-id',
        'instance-type',
        'local-ipv4',
        'local-hostname',
        'availability-zone',
        'mac',
        'vpc-id']);
    });
  });
});
