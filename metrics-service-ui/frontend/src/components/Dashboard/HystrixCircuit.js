/* eslint-disable prettier/prettier */
/* eslint-disable no-else-return */
/* eslint-disable camelcase */
import React from 'react';

export default function HystrixCircuit(props) {
    const {
        addCommas,
        rollingCountTimeout,
        propertyValue_executionIsolationStrategy,
        propertyValue_circuitBreakerForceClosed,
        errorPercentage,
        reportingHosts,
        getInstanceAverage,
        latencyExecute,
        latencyExecute_mean,
        isCircuitBreakerOpen,
        propertyValue_circuitBreakerForceOpen,
        rollingCountThreadPoolRejected,
        rollingCountSemaphoreRejected,
        rollingCountFailure,
        rollingCountSuccess,
        rollingCountBadRequests,
        ratePerSecondPerHost,
        ratePerSecond,
        roundNumber,
        rollingCountShortCircuited,
    } = props;
    return (
        `<div className="counters">
            <div className="cell line">
                <a
                    href="#disable"
                    title="Error Percentage [Timed-out + Threadpool Rejected + Failure / Total]"
                    className="tooltip errorPercentage"
                >
                    <span className="value">${errorPercentage}%</span>
                </a>
            </div>

            <div className="cell borderRight">
                <a href="#disable" title="Timed-out Request Count" className="line tooltip timeout">
                    ${' '}
                    ${addCommas(rollingCountTimeout)}${' '}
                </a>
                ${propertyValue_executionIsolationStrategy === 'THREAD' && `(
                    <a href="#disable" title="Threadpool Rejected Request Count" className="line tooltip rejected">
                        ${addCommas(rollingCountThreadPoolRejected)}
                    </a>
                )`}

                ${propertyValue_executionIsolationStrategy === 'SEMAPHORE' && (
                    `<a href="#disable" title="Semaphore Rejected Request Count" className="line tooltip rejected">
                        ${addCommas(rollingCountSemaphoreRejected)}
                    </a>`
                )}

                <a href="#disable" title="Failure Request Count" className="line tooltip failure">
                    ${addCommas(rollingCountFailure)}
                </a>
            </div>
            <div className="cell borderRight">
                <a href="#disable" title="Successful Request Count" className="line tooltip success">
                    ${addCommas(rollingCountSuccess)}
                </a>
                <a href="#disable" title="Short-circuited Request Count" className="line tooltip shortCircuited">
                    ${addCommas(rollingCountShortCircuited)}
                </a>
                <a href="#disable" title="Bad Request Count" className="line tooltip badRequest">
                    ${addCommas(rollingCountBadRequests)}
                </a>
                <br />
            </div>
        </div>

        <div className="rate">
            <a href="#disable" title="Total Request Rate per Second per Reporting Host" className="tooltip rate">
                <span className="smaller">Host: </span>
                <span className="ratePerSecondPerHost">${addCommas(roundNumber(ratePerSecondPerHost))}</span>/s
            </a>
        </div>
        <div className="rate">
            <a href="#disable" title="Total Request Rate per Second for Cluster" className="tooltip rate">
                <span className="smaller">Cluster: </span>
                <span className="ratePerSecond">${addCommas(roundNumber(ratePerSecond))}</span>/s
            </a>
        </div>

        <div className="circuitStatus">
            ${propertyValue_circuitBreakerForceClosed && (
                `<span className="smaller">
                    <font color="orange">Forced Closed</font>
                </span>`
            )}
            ${(() => {
                if (propertyValue_circuitBreakerForceOpen) {
                    return (
                        `<span>
                            Circuit <font color="red">Forced Open</font>
                        </span>`
                    );
                }
                if (isCircuitBreakerOpen === reportingHosts) {
                    return (
                        `<span>
                            Circuit <font color="red">Open</font>
                        </span>`
                    );
                } else if (isCircuitBreakerOpen === 0) {
                    return (
                        `<span>
                            Circuit <font color="green"> Closed</font>
                        </span>`
                    );
                } else if (typeof isCircuitBreakerOpen === 'object') {
                    return (
                        `<span>
                            Circuit <font color="red"> Open ${isCircuitBreakerOpen.true}</font>{' '}
                            <font color="green">Closed ${isCircuitBreakerOpen.false}</font>
                        </span>`
                    );
                } else {
                    return (
                        `<span>
                            Circuit${' '}
                            <font color="orange">
                                ${isCircuitBreakerOpen.toString().replace('true', 'Open').replace('false', 'Closed')}
                            </font>
                        </span>`
                    );
                }
            })()}
        </div>

        <div className="spacer" />

        <div className="tableRow">
            ${typeof reportingHosts !== 'undefined' ? (
                `<div className="cell header">Hosts</div>
                <div className="cell data">{reportingHosts}</div>`
            ) : (
                `<div className="cell header">Host</div>
                <div className="cell data">Single</div>`
            )}
            <div className="cell header">90th</div>
            <div className="cell data latency90">
                <span className="value">${getInstanceAverage(latencyExecute['90'], reportingHosts, false)}</span>ms
            </div>
        </div>
        <div className="tableRow">
            <div className="cell header">Median</div>
            <div className="cell data latencyMedian">
                <span className="value">${getInstanceAverage(latencyExecute['50'], reportingHosts, false)}</span>ms
            </div>
            <div className="cell header">99th</div>
            <div className="cell data latency99">
                <span className="value">${getInstanceAverage(latencyExecute['99'], reportingHosts, false)}</span>ms
            </div>
        </div>
        <div className="tableRow">
            <div className="cell header">Mean</div>
            <div className="cell data latencyMean">
                <span className="value">${latencyExecute_mean}</span>ms
            </div>
            <div className="cell header">99.5th</div>
            <div className="cell data latency995">
                <span className="value">${getInstanceAverage(latencyExecute['99.5'], reportingHosts, false)}</span>ms
            </div>
        </div>`)
}
