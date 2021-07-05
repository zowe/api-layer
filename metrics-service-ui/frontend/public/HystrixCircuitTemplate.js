/* eslint-disable camelcase */
/* eslint-disable no-else-return */
/* eslint-disable prettier/prettier */
window.HystrixCircuitTemplate = (props) => {
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
        `<div class="counters">
            <div class="cell line">
                <a
                    href="#disable"
                    title="Error Percentage [Timed-out + Threadpool Rejected + Failure / Total]"
                    class="tooltip errorPercentage"
                >
                    <span class="value">${errorPercentage}%</span>
                </a>
            </div>

            <div class="cell borderRight">
                <a href="#disable" title="Timed-out Request Count" class="line tooltip timeout">
                    ${' '}
                    ${addCommas(rollingCountTimeout)}${' '}
                </a>
                ${propertyValue_executionIsolationStrategy === 'THREAD' ? (`
                    <a href="#disable" title="Threadpool Rejected Request Count" class="line tooltip rejected">
                        ${addCommas(rollingCountThreadPoolRejected)}
                    </a>
                `): ''}

                ${propertyValue_executionIsolationStrategy === 'SEMAPHORE'? (
                    `<a href="#disable" title="Semaphore Rejected Request Count" class="line tooltip rejected">
                        ${addCommas(rollingCountSemaphoreRejected)}
                    </a>`
                ): ''}

                <a href="#disable" title="Failure Request Count" class="line tooltip failure">
                    ${addCommas(rollingCountFailure)}
                </a>
            </div>
            <div class="cell borderRight">
                <a href="#disable" title="Successful Request Count" class="line tooltip success">
                    ${addCommas(rollingCountSuccess)}
                </a>
                <a href="#disable" title="Short-circuited Request Count" class="line tooltip shortCircuited">
                    ${addCommas(rollingCountShortCircuited)}
                </a>
                <a href="#disable" title="Bad Request Count" class="line tooltip badRequest">
                    ${addCommas(rollingCountBadRequests)}
                </a>
                <br />
            </div>
        </div>

        <div class="rate">
            <a href="#disable" title="Total Request Rate per Second per Reporting Host" class="tooltip rate">
                <span class="smaller">Host: </span>
                <span class="ratePerSecondPerHost">${addCommas(roundNumber(ratePerSecondPerHost))}</span>/s
            </a>
        </div>
        <div class="rate">
            <a href="#disable" title="Total Request Rate per Second for Cluster" class="tooltip rate">
                <span class="smaller">Cluster: </span>
                <span class="ratePerSecond">${addCommas(roundNumber(ratePerSecond))}</span>/s
            </a>
        </div>

        <div class="circuitStatus">
            ${propertyValue_circuitBreakerForceClosed? (
                `<span class="smaller">
                    <font color="orange">Forced Closed</font>
                </span>`
            ): ''}
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

        <div class="spacer" />

        <div class="tableRow">
            ${typeof reportingHosts !== 'undefined' ? (
                `<div class="cell header">Hosts</div>
                <div class="cell data">${reportingHosts}</div>`
            ) : (
                `<div class="cell header">Host</div>
                <div class="cell data">Single</div>`
            )}
            <div class="cell header">90th</div>
            <div class="cell data latency90">
                <span class="value">${getInstanceAverage(latencyExecute['90'], reportingHosts, false)}</span>ms
            </div>
        </div>
        <div class="tableRow">
            <div class="cell header">Median</div>
            <div class="cell data latencyMedian">
                <span class="value">${getInstanceAverage(latencyExecute['50'], reportingHosts, false)}</span>ms
            </div>
            <div class="cell header">99th</div>
            <div class="cell data latency99">
                <span class="value">${getInstanceAverage(latencyExecute['99'], reportingHosts, false)}</span>ms
            </div>
        </div>
        <div class="tableRow">
            <div class="cell header">Mean</div>
            <div class="cell data latencyMean">
                <span class="value">${latencyExecute_mean}</span>ms
            </div>
            <div class="cell header">99.5th</div>
            <div class="cell data latency995">
                <span class="value">${getInstanceAverage(latencyExecute['99.5'], reportingHosts, false)}</span>ms
            </div>
        </div>`)
}
