import defaults from 'lodash/defaults';

import React, { ChangeEvent, PureComponent } from 'react';
import { LegacyForms } from '@grafana/ui';
import { QueryEditorProps } from '@grafana/data';
import { DataSource } from './datasource';
import { defaultQuery, MyDataSourceOptions, MyQuery } from './types';

const { FormField } = LegacyForms;

type Props = QueryEditorProps<DataSource, MyQuery, MyDataSourceOptions>;

export class QueryEditor extends PureComponent<Props> {
  // onConstantChange = (event: ChangeEvent<HTMLInputElement>) => {
  //   const { onChange, query, onRunQuery } = this.props;
  //   onChange({ ...query, cluster: parseFloat(event.target.value) });
  //   // executes the query
  //   onRunQuery();
  // };

  onQueryTextChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { onChange, query } = this.props;
    onChange({ ...query, metricName: event.target.value });
  };

  onClusterChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { onChange, query } = this.props;
    onChange({ ...query, cluster: event.target.value });
  };

  render() {
    const query = defaults(this.props.query, defaultQuery);
    const { cluster, metricName } = query;

    return (
      <div className="gf-form">
        <FormField width={8} value={cluster || ''} onChange={this.onClusterChange} label="Cluster" />
        <FormField labelWidth={8} value={metricName || ''} onChange={this.onQueryTextChange} label="Metric Name" />
      </div>
    );
  }
}
