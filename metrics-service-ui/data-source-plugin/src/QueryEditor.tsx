import defaults from 'lodash/defaults';

import React, { ChangeEvent, PureComponent } from 'react';
import { LegacyForms } from '@grafana/ui';
import { QueryEditorProps, SelectableValue } from '@grafana/data';
import { DataSource } from './datasource';
import { defaultQuery, MyDataSourceOptions, MyQuery } from './types';

const { FormField } = LegacyForms;
const config = require('../config/default.json');
const availableClustersEndpoint = config.availableClustersEndpoint;

type Props = QueryEditorProps<DataSource, MyQuery, MyDataSourceOptions>;
type State = { availableClusters: JSX.Element[] };

export class QueryEditor extends PureComponent<Props, State> {
    onQueryTextChange = (event: ChangeEvent<HTMLInputElement>) => {
        const { onChange, query } = this.props;
        onChange({ ...query, metricName: event.target.value });
    };

    onClusterChange = (event: SelectableValue<HTMLInputElement>) => {
        const { onChange, query } = this.props;
        onChange({ ...query, clusterName: event.target.value });
    };

    async componentDidMount() {
        fetch(availableClustersEndpoint)
            .then((resp) => resp.json())
            .then((clusters) => {
                const availableClusters: JSX.Element[] = [];

                // Add empty starting item to dropdown menu
                availableClusters.push(<option key="" value="" />);

                // Add clusters from rest call to dropdown menu
                clusters.forEach((cluster: { name: string; link: string }) => {
                    availableClusters.push(
                        <option key={cluster.name} value={cluster.name}>
                            {cluster.name}
                        </option>
                    );
                });

                this.setState({ availableClusters });
            });
    }

    render() {
        const query = defaults(this.props.query, defaultQuery);

        let availableClusters: JSX.Element[] = [];
        const clusterName = query.clusterName || '';
        const metricName = query.metricName || '';

        if (this.state != null) {
            availableClusters = this.state.availableClusters;
        }

        return (
            <div className="gf-form">
                <select onChange={this.onClusterChange} value={clusterName}>
                    {availableClusters}
                </select>
                <FormField labelWidth={8} value={metricName} onChange={this.onQueryTextChange} label="Metric Name" />
            </div>
        );
    }
}
