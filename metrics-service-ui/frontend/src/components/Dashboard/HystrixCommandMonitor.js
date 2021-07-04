/* eslint-disable */
import * as d3 from 'd3';
import $ from 'jquery';
import HystrixCircuit from './HystrixCircuit';
import HystrixCircuitContainer from './HystrixCircuitContainer';
import { renderToString } from 'react-dom/server';
import tinysort from 'tinysort';

export default class HystrixCommandMonitor {

    constructor(index, containerId, args) {
        console.log("Hystrix initialised!");
        this.args = args;
        if (this.args === undefined) {
            this.args = {};
        }
        this.index = index;
        this.containerId = containerId;
        // default sort type and direction
        this.sortedBy = 'alph_asc';

        /**
         * Initialization on construction
         */
        // constants used for visualization
        this.maxXaxisForCircle = "40%";
        this.maxYaxisForCircle = "40%";
        this.maxRadiusForCircle = "125";

        this.circuitCircleRadius = d3.scalePow().exponent(0.5).domain([0, 400]).range(["5", this.maxRadiusForCircle]); // requests per second per host
        this.circuitCircleYaxis = d3.scaleLinear().domain([0, 400]).range(["30%", this.maxXaxisForCircle]);
        this.circuitCircleXaxis = d3.scaleLinear().domain([0, 400]).range(["30%", this.maxYaxisForCircle]);
        this.circuitColorRange = d3.scaleLinear().domain([10, 25, 40, 50]).range(["green", "#FFCC00", "#FF9900", "red"]);
        this.circuitErrorPercentageColorRange = d3.scaleLinear().domain([0, 10, 35, 50]).range(["grey", "black", "#FF9900", "red"]);

        /**
         * We want to keep sorting in the background since data values are always changing, so this will re-sort every X milliseconds
         * to maintain whatever sort the user (or default) has chosen.
         * 
         * In other words, sorting only for adds/deletes is not sufficient as all but alphabetical sort are dynamically changing.
         */
        setInterval(() => {
            // sort since we have added a new one
            this.sortSameAsLast();
        }, 10000);
    }

    /**
 * Event listener to handle new messages from EventSource as streamed from the server.
 */
    eventSourceMessageListener = (e) => {
        var data = JSON.parse(e.data);
        if (data) {
            data.index = this.index;
            // check for reportingHosts (if not there, set it to 1 for singleHost vs cluster)
            if (!data.reportingHosts) {
                data.reportingHosts = 1;
            }

            if (data && data.type == 'HystrixCommand') {
                if (data.deleteData == 'true') {
                    this.deleteCircuit(data.escapedName);
                } else {
                    console.log('Display Circuit');
                    this.displayCircuit(data);
                }
            }
        }
    };

    /**
     * Pre process the data before displying in the UI. 
     * e.g   Get Averages from sums, do rate calculation etc. 
     */
    preProcessData = (data) => {
        // set defaults for values that may be missing from older streams
        this.setIfMissing(data, "rollingCountBadRequests", 0);
        // assert all the values we need
        this.validateData(data);
        // escape string used in jQuery & d3 selectors
        data.escapedName = data.name.replace(/([ !"#$%&'()*+,./:;<=>?@[\]^`{|}~])/g, '\\$1') + '_' + data.index;
        // do math
        this.convertAllAvg(data);
        this.calcRatePerSecond(data);
    }

    setIfMissing = (data, key, defaultValue) => {
        if (data[key] == undefined) {
            data[key] = defaultValue;
        }
    }

    /**
     * Since the stream of data can be aggregated from multiple hosts in a tiered manner
     * the aggregation just sums everything together and provides us the denominator (reportingHosts)
     * so we must divide by it to get an average per instance value. 
     * 
     * We want to do this on any numerical values where we want per instance rather than cluster-wide sum.
     */
    convertAllAvg = (data) => {
        this.convertAvg(data, "errorPercentage", true);
        this.convertAvg(data, "latencyExecute_mean", false);
    }

    convertAvg = (data, key, decimal) => {
        if (decimal) {
            data[key] = this.getInstanceAverage(data[key], data["reportingHosts"], decimal);
        } else {
            data[key] = this.getInstanceAverage(data[key], data["reportingHosts"], decimal);
        }
    }

    getInstanceAverage = (value, reportingHosts, decimal) => {
        if (decimal) {
            return this.roundNumber(value / reportingHosts);
        } else {
            return Math.floor(value / reportingHosts);
        }
    }

    calcRatePerSecond = (data) => {
        var numberSeconds = data["propertyValue_metricsRollingStatisticalWindowInMilliseconds"] / 1000;

        var totalRequests = data["requestCount"];
        if (totalRequests < 0) {
            totalRequests = 0;
        }
        data["ratePerSecond"] = this.roundNumber(totalRequests / numberSeconds);
        data["ratePerSecondPerHost"] = this.roundNumber(totalRequests / numberSeconds / data["reportingHosts"]);
    }

    validateData = (data) => {
        this.assertNotNull(data, "reportingHosts");
        this.assertNotNull(data, "type");
        this.assertNotNull(data, "name");
        this.assertNotNull(data, "group");
        // this.assertNotNull(data,"currentTime");
        this.assertNotNull(data, "isCircuitBreakerOpen");
        this.assertNotNull(data, "errorPercentage");
        this.assertNotNull(data, "errorCount");
        this.assertNotNull(data, "requestCount");
        this.assertNotNull(data, "rollingCountCollapsedRequests");
        this.assertNotNull(data, "rollingCountExceptionsThrown");
        this.assertNotNull(data, "rollingCountFailure");
        this.assertNotNull(data, "rollingCountFallbackFailure");
        this.assertNotNull(data, "rollingCountFallbackRejection");
        this.assertNotNull(data, "rollingCountFallbackSuccess");
        this.assertNotNull(data, "rollingCountResponsesFromCache");
        this.assertNotNull(data, "rollingCountSemaphoreRejected");
        this.assertNotNull(data, "rollingCountShortCircuited");
        this.assertNotNull(data, "rollingCountSuccess");
        this.assertNotNull(data, "rollingCountThreadPoolRejected");
        this.assertNotNull(data, "rollingCountTimeout");
        this.assertNotNull(data, "rollingCountBadRequests");
        this.assertNotNull(data, "currentConcurrentExecutionCount");
        this.assertNotNull(data, "latencyExecute_mean");
        this.assertNotNull(data, "latencyExecute");
        this.assertNotNull(data, "propertyValue_circuitBreakerRequestVolumeThreshold");
        this.assertNotNull(data, "propertyValue_circuitBreakerSleepWindowInMilliseconds");
        this.assertNotNull(data, "propertyValue_circuitBreakerErrorThresholdPercentage");
        this.assertNotNull(data, "propertyValue_circuitBreakerForceOpen");
        this.assertNotNull(data, "propertyValue_circuitBreakerForceClosed");
        this.assertNotNull(data, "propertyValue_executionIsolationStrategy");
        this.assertNotNull(data, "propertyValue_executionIsolationThreadTimeoutInMilliseconds");
        this.assertNotNull(data, "propertyValue_executionIsolationThreadInterruptOnTimeout");
        // this.assertNotNull(data,"propertyValue_executionIsolationThreadPoolKeyOverride");
        this.assertNotNull(data, "propertyValue_executionIsolationSemaphoreMaxConcurrentRequests");
        this.assertNotNull(data, "propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests");
        this.assertNotNull(data, "propertyValue_requestCacheEnabled");
        this.assertNotNull(data, "propertyValue_requestLogEnabled");
        this.assertNotNull(data, "propertyValue_metricsRollingStatisticalWindowInMilliseconds");
    }

    assertNotNull = (data, key) => {
        if (data[key] == undefined) {
            throw new Error("Key Missing: " + key + " for " + data.name);
        }
    }

    /**
     * Method to display the CIRCUIT data
     * 
     * @param data
     */
    /* private */
    displayCircuit = (data) => {

        try {
            this.preProcessData(data);
        } catch (err) {
            log("Failed preProcessData: " + err.message);
            return;
        }

        // add the 'addCommas' function to the 'data' object so the HTML templates can use it
        data.addCommas = addCommas;
        // add the 'roundNumber' function to the 'data' object so the HTML templates can use it
        data.roundNumber = this.roundNumber;
        // add the 'getInstanceAverage' function to the 'data' object so the HTML templates can use it
        data.getInstanceAverage = this.getInstanceAverage;

        var addNew = false;
        // check if we need to create the container
        if (!$('#CIRCUIT_' + data.escapedName).length) {
            // args for display
            if (this.args.includeDetailIcon != undefined && this.args.includeDetailIcon) {
                data.includeDetailIcon = true;
            } else {
                data.includeDetailIcon = false;
            }

            // it doesn't exist so add it
            var html = renderToString(<HystrixCircuitContainer {...data}/>);
            // remove the loading thing first
            $('#' + this.containerId + ' span.loading').remove();
            // now create the new data and add it
            $('#' + this.containerId + '').append(html);

            let y = 200;
            /* escape with two backslashes */
            const vis = d3
                .select(`#chart_CIRCUIT_${`${data.name.replace(/([ !"#$%&'()*+,./:;<=>?@[\]^`{|}~])/g, '\\\\$1')}_${data.index}`}`)
                .append('svg:svg')
                .attr('width', '100%')
                .attr('height', '100%');
            /* add a circle -- we don't use the data point, we set it manually, so just passing in [1] */
            const circle = vis.selectAll('circle').data([1]).enter().append('svg:circle');
            /* setup the initial styling and sizing of the circle */
            circle.style('fill', 'green').attr('cx', '30%').attr('cy', '30%').attr('r', 5);

            /* add the line graph - it will be populated by javascript, no default to show here */
            /* escape with two backslashes */
            let graph = d3
                .select(`#graph_CIRCUIT_${`${data.name.replace(/([ !"#$%&'()*+,./:;<=>?@[\]^`{|}~])/g, '\\\\$1')}_${data.index}`}`)
                .append('svg:svg')
                .attr('width', '100%')
                .attr('height', '100%');
            console.log(graph);
            console.log(`#graph_CIRCUIT_${`${data.name.replace(/([ !"#$%&'()*+,./:;<=>?@[\]^`{|}~])/g, '\\\\$1')}_${data.index}`}`);
            window.d3 = d3;

            console.log($('#graph_CIRCUIT_' + data.escapedName + ' svg'));

            // add the default sparkline graph
            d3.selectAll('#graph_CIRCUIT_' + data.escapedName + ' svg').append("svg:path");

            // remember this is new so we can trigger a sort after setting data
            addNew = true;
        }

        console.log(data.escapedName);

        // now update/insert the data
        $('#CIRCUIT_' + data.escapedName + ' div.monitor_data').html(renderToString(<HystrixCircuit {...data}/>));

        var ratePerSecond = data.ratePerSecond;
        var ratePerSecondPerHost = data.ratePerSecondPerHost;
        var ratePerSecondPerHostDisplay = ratePerSecondPerHost;
        var errorThenVolume = isNaN(ratePerSecond) ? -1 : (data.errorPercentage * 100000000) + ratePerSecond;
        // set the rates on the div element so it's available for sorting
        $('#CIRCUIT_' + data.escapedName).attr('rate_value', ratePerSecond);
        $('#CIRCUIT_' + data.escapedName).attr('error_then_volume', errorThenVolume);

        // update errorPercentage color on page
        $('#CIRCUIT_' + data.escapedName + ' a.errorPercentage').css('color', this.circuitErrorPercentageColorRange(data.errorPercentage));

        console.log('#CIRCUIT_' + data.escapedName + ' circle');

        this.updateCircle('circuit', '#CIRCUIT_' + data.escapedName + ' circle', ratePerSecondPerHostDisplay, data.errorPercentage);

        console.log(data.graphValues);
        if (data.graphValues) {
            // we have a set of values to initialize with
            console.log($('#CIRCUIT_' + data.escapedName + ' path'));
            console.log(data.graphValues);
            this.updateSparkline('circuit', '#CIRCUIT_' + data.escapedName + ' path', data.graphValues);
        } else {
            console.log($('#CIRCUIT_' + data.escapedName + ' path'));
            this.updateSparkline('circuit', '#CIRCUIT_' + data.escapedName + ' path', ratePerSecond);
        }

        if (addNew) {
            // sort since we added a new circuit
            this.sortSameAsLast();
        }
    }

    /* round a number to X digits: num => the number to round, dec => the number of decimals */
    /* private */
    roundNumber = (num) => {
        var dec = 1;
        var result = Math.round(num * Math.pow(10, dec)) / Math.pow(10, dec);
        var resultAsString = result.toString();
        if (resultAsString.indexOf('.') == -1) {
            resultAsString = resultAsString + '.0';
        }
        return resultAsString;
    };




    /* private */
    updateCircle = (variablePrefix, cssTarget, rate, errorPercentage) => {
        var newXaxisForCircle = this[variablePrefix + 'CircleXaxis'](rate);
        if (parseInt(newXaxisForCircle) > parseInt(this.maxXaxisForCircle)) {
            newXaxisForCircle = this.maxXaxisForCircle;
        }
        var newYaxisForCircle = this[variablePrefix + 'CircleYaxis'](rate);
        if (parseInt(newYaxisForCircle) > parseInt(this.maxYaxisForCircle)) {
            newYaxisForCircle = this.maxYaxisForCircle;
        }
        var newRadiusForCircle = this[variablePrefix + 'CircleRadius'](rate);
        if (parseInt(newRadiusForCircle) > parseInt(this.maxRadiusForCircle)) {
            newRadiusForCircle = this.maxRadiusForCircle;
        }

        d3.selectAll(cssTarget)
            .transition()
            .duration(400)
            .attr("cy", newYaxisForCircle)
            .attr("cx", newXaxisForCircle)
            .attr("r", newRadiusForCircle)
            .style("fill", this[variablePrefix + 'ColorRange'](errorPercentage));
    }

    /* private */
    updateSparkline = (variablePrefix, cssTarget, newDataPoint) => {
        var currentTimeMilliseconds = new Date().getTime();
        var data = self[variablePrefix + cssTarget + '_data'];
        if (typeof data == 'undefined') {
            // else it's new
            if (typeof newDataPoint == 'object') {
                // we received an array of values, so initialize with it
                data = newDataPoint;
            } else {
                // v: VALUE, t: TIME_IN_MILLISECONDS
                data = [{ "v": parseFloat(newDataPoint), "t": currentTimeMilliseconds }];
            }
            this[variablePrefix + cssTarget + '_data'] = data;
        } else {
            if (typeof newDataPoint == 'object') {
                /* if an array is passed in we'll replace the cached one */
                data = newDataPoint;
            } else {
                // else we just add to the existing one
                data.push({ "v": parseFloat(newDataPoint), "t": currentTimeMilliseconds });
            }
        }

        while (data.length > 200) { // 400 should be plenty for the 2 minutes we have the scale set to below even with a very low update latency
            // remove data so we don't keep increasing forever 
            data.shift();
        }

        if (data.length == 1 && data[0].v == 0) {
            //console.log("we have a single 0 so skipping");
            // don't show if we have a single 0
            return;
        }

        if (data.length > 1 && data[0].v == 0 && data[1].v != 0) {
            //console.log("we have a leading 0 so removing it");
            // get rid of a leading 0 if the following number is not a 0
            data.shift();
        }

        var xScale = d3.scaleTime().domain([new Date(currentTimeMilliseconds - (60 * 1000 * 2)), new Date(currentTimeMilliseconds)]).range([0, 140]);

        var yMin = d3.min(data, function (d) { return d.v; });
        var yMax = d3.max(data, function (d) { return d.v; });
        var yScale = d3.scaleLinear().domain([yMin, yMax]).nice().range([60, 0]); // y goes DOWN, so 60 is the "lowest"

        const sparkline = d3.line()
            // assign the X function to plot our line as we wish
            .x(function (d, i) {
                // return the X coordinate where we want to plot this datapoint based on the time
                return xScale(new Date(d.t));
            })
            .y(function (d) {
                return yScale(d.v);
            })
            .curve(d3.curveBasis);

        d3.selectAll(cssTarget).attr("d", sparkline(data));
    }

    /* private */
    deleteCircuit = (circuitName) => {
        $('#CIRCUIT_' + circuitName).remove();
    }

    // public methods for sorting
    sortByVolume = () => {
        var direction = "desc";
        if (this.sortedBy == 'rate_desc') {
            direction = 'asc';
        }
        this.sortByVolumeInDirection(direction);
    }

    sortByVolumeInDirection = (direction) => {
        this.sortedBy = 'rate_' + direction;
        tinysort($('#' + this.containerId + ' div.monitor'), { order: direction, attr: 'rate_value' });
    };

    sortAlphabetically = () => {
        var direction = "asc";
        if (this.sortedBy == 'alph_asc') {
            direction = 'desc';
        }
        this.sortAlphabeticalInDirection(direction);
    };

    sortAlphabeticalInDirection = (direction) => {
        this.sortedBy = 'alph_' + direction;
        tinysort($('#' + this.containerId + ' div.monitor'), "p.name", { order: direction });
    };


    sortByError = () => {
        var direction = "desc";
        if (this.sortedBy == 'error_desc') {
            direction = 'asc';
        }
        this.sortByErrorInDirection(direction);
    };

    sortByErrorInDirection = (direction) => {
        this.sortedBy = 'error_' + direction;
        tinysort($('#' + this.containerId + ' div.monitor'), ".errorPercentage .value", { order: direction });
    };

    sortByErrorThenVolume = () => {
        var direction = "desc";
        if (this.sortedBy == 'error_then_volume_desc') {
            direction = 'asc';
        }
        this.sortByErrorThenVolumeInDirection(direction);
    };

    sortByErrorThenVolumeInDirection = (direction) => {
        this.sortedBy = 'error_then_volume_' + direction;
        tinysort($('#' + this.containerId + ' div.monitor'), { order: direction, attr: 'error_then_volume' });
    };

    sortByLatency90 = () => {
        var direction = "desc";
        if (this.sortedBy == 'lat90_desc') {
            direction = 'asc';
        }
        this.sortedBy = 'lat90_' + direction;
        this.sortByMetricInDirection(direction, ".latency90 .value");
    };

    sortByLatency99 = () => {
        var direction = "desc";
        if (this.sortedBy == 'lat99_desc') {
            direction = 'asc';
        }
        this.sortedBy = 'lat99_' + direction;
        this.sortByMetricInDirection(direction, ".latency99 .value");
    };

    sortByLatency995 = () => {
        var direction = "desc";
        if (this.sortedBy == 'lat995_desc') {
            direction = 'asc';
        }
        this.sortedBy = 'lat995_' + direction;
        this.sortByMetricInDirection(direction, ".latency995 .value");
    };

    sortByLatencyMean = () => {
        var direction = "desc";
        if (this.sortedBy == 'latMean_desc') {
            direction = 'asc';
        }
        this.sortedBy = 'latMean_' + direction;
        this.sortByMetricInDirection(direction, ".latencyMean .value");
    };

    sortByLatencyMedian = () => {
        var direction = "desc";
        if (this.sortedBy == 'latMedian_desc') {
            direction = 'asc';
        }
        this.sortedBy = 'latMedian_' + direction;
        this.sortByMetricInDirection(direction, ".latencyMedian .value");
    };

    sortByMetricInDirection = (direction, metric) => {
        tinysort($('#' + this.containerId + ' div.monitor'), metric, { order: direction });
    };

    // this method is for when new divs are added to cause the elements to be sorted to whatever the user last chose
    sortSameAsLast = () => {
        if (this.sortedBy == 'alph_asc') {
            this.sortAlphabeticalInDirection('asc');
        } else if (this.sortedBy == 'alph_desc') {
            this.sortAlphabeticalInDirection('desc');
        } else if (this.sortedBy == 'rate_asc') {
            this.sortByVolumeInDirection('asc');
        } else if (this.sortedBy == 'rate_desc') {
            this.sortByVolumeInDirection('desc');
        } else if (this.sortedBy == 'error_asc') {
            this.sortByErrorInDirection('asc');
        } else if (this.sortedBy == 'error_desc') {
            this.sortByErrorInDirection('desc');
        } else if (this.sortedBy == 'error_then_volume_asc') {
            this.sortByErrorThenVolumeInDirection('asc');
        } else if (this.sortedBy == 'error_then_volume_desc') {
            this.sortByErrorThenVolumeInDirection('desc');
        } else if (this.sortedBy == 'lat90_asc') {
            this.sortByMetricInDirection('asc', '.latency90 .value');
        } else if (this.sortedBy == 'lat90_desc') {
            this.sortByMetricInDirection('desc', '.latency90 .value');
        } else if (this.sortedBy == 'lat99_asc') {
            this.sortByMetricInDirection('asc', '.latency99 .value');
        } else if (this.sortedBy == 'lat99_desc') {
            this.sortByMetricInDirection('desc', '.latency99 .value');
        } else if (this.sortedBy == 'lat995_asc') {
            this.sortByMetricInDirection('asc', '.latency995 .value');
        } else if (this.sortedBy == 'lat995_desc') {
            this.sortByMetricInDirection('desc', '.latency995 .value');
        } else if (this.sortedBy == 'latMean_asc') {
            this.sortByMetricInDirection('asc', '.latencyMean .value');
        } else if (this.sortedBy == 'latMean_desc') {
            this.sortByMetricInDirection('desc', '.latencyMean .value');
        } else if (this.sortedBy == 'latMedian_asc') {
            this.sortByMetricInDirection('asc', '.latencyMedian .value');
        } else if (this.sortedBy == 'latMedian_desc') {
            this.sortByMetricInDirection('desc', '.latencyMedian .value');
        }
    };
}

// a temporary home for the logger until we become more sophisticated
const log = (message) => {
    console.log(message);
};

const addCommas = (nStr) => {
    nStr += '';
    if (nStr.length <= 3) {
        return nStr; //shortcut if we don't need commas
    }
    x = nStr.split('.');
    x1 = x[0];
    x2 = x.length > 1 ? '.' + x[1] : '';
    var rgx = /(\d+)(\d{3})/;
    while (rgx.test(x1)) {
        x1 = x1.replace(rgx, '$1' + ',' + '$2');
    }
    return x1 + x2;
}