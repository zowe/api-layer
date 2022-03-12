import defaults from 'lodash/defaults';

import React, { ChangeEvent, PureComponent } from 'react';
import { LegacyForms } from '@grafana/ui';
import { QueryEditorProps, SelectableValue } from '@grafana/data';
import { DataSource } from './datasource';
import { defaultQuery, MyDataSourceOptions, MyQuery } from './types';

const { FormField } = LegacyForms;
const availableClustersEndpoint = 'https://localhost:10010/metrics-service/api/v1/clusters';

type Props = QueryEditorProps<DataSource, MyQuery, MyDataSourceOptions>;

export class QueryEditor extends PureComponent<Props> {
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
        let items: JSX.Element[] = [];

        // Add empty starting item to dropdown menu
        items.push(<option key="" value="" />);

        // Add clusters from rest call to dropdown menu
        clusters.forEach((cluster: { name: string; link: string }) => {
          items.push(
            <option key={cluster.name} value={cluster.name}>
              {cluster.name}
            </option>
          );
        });

        this.setState({ items });
      });
  }

  render() {
    const query = defaults(this.props.query, defaultQuery);

    let availableClusters = '';

    if (this.state != null) {
      // @ts-ignore
      availableClusters = this.state.items;
    }

    return (
      <div className="gf-form">
        <select onChange={this.onClusterChange} value={query.clusterName || ''}>
          {availableClusters}
        </select>
        <FormField
          labelWidth={8}
          value={query.metricName || ''}
          onChange={this.onQueryTextChange}
          label="Metric Name"
        />
      </div>
    );
  }
}
