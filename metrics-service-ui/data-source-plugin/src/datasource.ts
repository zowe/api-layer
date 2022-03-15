import defaults from 'lodash/defaults';
import { Observable, merge } from 'rxjs';

import {
    DataQueryRequest,
    DataQueryResponse,
    DataSourceApi,
    DataSourceInstanceSettings,
    FieldType,
    CircularDataFrame,
    LoadingState,
} from '@grafana/data';

import { MyQuery, MyDataSourceOptions, defaultQuery } from './types';

const config = require('../config/default.json');
const metricsServiceEndpoint = config.metricsServiceEndpoint;

export class DataSource extends DataSourceApi<MyQuery, MyDataSourceOptions> {
    constructor(instanceSettings: DataSourceInstanceSettings<MyDataSourceOptions>) {
        super(instanceSettings);
    }

    query(options: DataQueryRequest<MyQuery>): Observable<DataQueryResponse> {
        const streams = options.targets.map((target) => {
            const query = defaults(target, defaultQuery);

            return new Observable<DataQueryResponse>((subscriber) => {
                const frame = new CircularDataFrame({
                    append: 'tail',
                    capacity: 1000,
                });

                frame.refId = query.refId;
                frame.addField({ name: 'time', type: FieldType.time });
                frame.addField({ name: 'value', type: FieldType.number });

                const metricClusterUrl = metricsServiceEndpoint + '/sse/v1/turbine.stream?cluster=' + query.clusterName;

                const eventSource = new EventSource(metricClusterUrl, { withCredentials: true });
                eventSource.addEventListener('message', (event) => {
                    // "event.data" is a string
                    const data = JSON.parse(event.data);
                    frame.add({ time: Date.now(), value: data[query.metricName] });
                    subscriber.next({
                        data: [frame],
                        key: query.refId,
                        state: LoadingState.Streaming,
                    });
                });

                return () => {
                    eventSource.close();
                };
            });
        });

        return merge(...streams);
    }

    testDatasource(): Promise<any> {
        return Promise.resolve(undefined);
    }
}
