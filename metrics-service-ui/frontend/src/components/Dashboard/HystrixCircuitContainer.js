/* eslint-disable no-console */
/* eslint-disable prefer-const */
/* eslint-disable no-unused-vars */
import React, { useEffect } from 'react';
import * as d3 from 'd3';

export default function HystrixCircuitContainer(props) {
    const { name, index } = props;
    let displayName = name;
    let toolTip = '';
    if (displayName.length > 32) {
        displayName = `${displayName.substring(0, 4)}...${displayName.substring(
            displayName.length - 20,
            displayName.length
        )}`;
        toolTip = `title="${name}"`;
    }

    return (
        <React.Fragment>
            <div className="monitor" id={`CIRCUIT_${`${name}_${index}`}`} style={{ position: 'relative' }}>
                <div
                    id={`chart_CIRCUIT_${`${name}_${index}`}`}
                    className="chart"
                    style={{
                        position: 'absolute',
                        top: '0px',
                        left: '0',
                        float: 'left',
                        width: '100%',
                        height: '100%',
                    }}
                />
                <div
                    style={{
                        position: 'absolute',
                        top: '0',
                        width: '100%',
                        height: '15px',
                        opacity: '0.8',
                        background: 'white',
                    }}
                >
                    <p className="name" />
                </div>
                <div
                    style={{
                        position: 'absolute',
                        top: '15px',
                        opacity: '0.8',
                        background: 'white',
                        width: '100%',
                        height: '95%',
                    }}
                >
                    <div className="monitor_data" />
                </div>
                <div
                    id={`graph_CIRCUIT_${`${name}_${index}`}`}
                    className="graph"
                    style={{
                        position: 'absolute',
                        top: '25px',
                        left: '0',
                        float: 'left',
                        width: '140px',
                        height: '62px',
                    }}
                />
            </div>
        </React.Fragment>
    );
}
