/* eslint-disable */
import * as d3 from 'd3';
import $ from 'jquery';

export default class HystrixCommandMonitor {

    constructor(index, containerId, args) {
        console.log("Hystrix initialised!");
        this.args = args;
        if (this.args === undefined) {
            this.args = {};
        }
        this.index = index;
        this.containerId = containerId;

        /**
         * Initialization on construction
         */
        // constants used for visualization
        const maxXaxisForCircle = "40%";
        const maxYaxisForCircle = "40%";
        const maxRadiusForCircle = "125";

        this.circuitCircleRadius = d3.scalePow().exponent(0.5).domain([0, 400]).range(["5", maxRadiusForCircle]); // requests per second per host
        this.circuitCircleYaxis = d3.scaleLinear().domain([0, 400]).range(["30%", maxXaxisForCircle]);
        this.circuitCircleXaxis = d3.scaleLinear().domain([0, 400]).range(["30%", maxYaxisForCircle]);
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
    eventSourceMessageListener = function (e) {
        var data = JSON.parse(e.data);
        if (data) {
            data.index = this.index;
            // check for reportingHosts (if not there, set it to 1 for singleHost vs cluster)
            if (!data.reportingHosts) {
                data.reportingHosts = 1;
            }

            if (data && data.type == 'HystrixCommand') {
                if (data.deleteData == 'true') {
                    deleteCircuit(data.escapedName);
                } else {
                    displayCircuit(data);
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
        setIfMissing(data, "rollingCountBadRequests", 0);
        // assert all the values we need
        validateData(data);
        // escape string used in jQuery & d3 selectors
        data.escapedName = data.name.replace(/([ !"#$%&'()*+,./:;<=>?@[\]^`{|}~])/g, '\\$1') + '_' + data.index;
        // do math
        convertAllAvg(data);
        calcRatePerSecond(data);
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
        convertAvg(data, "errorPercentage", true);
        convertAvg(data, "latencyExecute_mean", false);
    }

    convertAvg = (data, key, decimal) => {
        if (decimal) {
            data[key] = getInstanceAverage(data[key], data["reportingHosts"], decimal);
        } else {
            data[key] = getInstanceAverage(data[key], data["reportingHosts"], decimal);
        }
    }

    getInstanceAverage = (value, reportingHosts, decimal) => {
        if (decimal) {
            return roundNumber(value / reportingHosts);
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
        data["ratePerSecond"] = roundNumber(totalRequests / numberSeconds);
        data["ratePerSecondPerHost"] = roundNumber(totalRequests / numberSeconds / data["reportingHosts"]);
    }

    validateData = (data) => {
        assertNotNull(data, "reportingHosts");
        assertNotNull(data, "type");
        assertNotNull(data, "name");
        assertNotNull(data, "group");
        // assertNotNull(data,"currentTime");
        assertNotNull(data, "isCircuitBreakerOpen");
        assertNotNull(data, "errorPercentage");
        assertNotNull(data, "errorCount");
        assertNotNull(data, "requestCount");
        assertNotNull(data, "rollingCountCollapsedRequests");
        assertNotNull(data, "rollingCountExceptionsThrown");
        assertNotNull(data, "rollingCountFailure");
        assertNotNull(data, "rollingCountFallbackFailure");
        assertNotNull(data, "rollingCountFallbackRejection");
        assertNotNull(data, "rollingCountFallbackSuccess");
        assertNotNull(data, "rollingCountResponsesFromCache");
        assertNotNull(data, "rollingCountSemaphoreRejected");
        assertNotNull(data, "rollingCountShortCircuited");
        assertNotNull(data, "rollingCountSuccess");
        assertNotNull(data, "rollingCountThreadPoolRejected");
        assertNotNull(data, "rollingCountTimeout");
        assertNotNull(data, "rollingCountBadRequests");
        assertNotNull(data, "currentConcurrentExecutionCount");
        assertNotNull(data, "latencyExecute_mean");
        assertNotNull(data, "latencyExecute");
        assertNotNull(data, "propertyValue_circuitBreakerRequestVolumeThreshold");
        assertNotNull(data, "propertyValue_circuitBreakerSleepWindowInMilliseconds");
        assertNotNull(data, "propertyValue_circuitBreakerErrorThresholdPercentage");
        assertNotNull(data, "propertyValue_circuitBreakerForceOpen");
        assertNotNull(data, "propertyValue_circuitBreakerForceClosed");
        assertNotNull(data, "propertyValue_executionIsolationStrategy");
        assertNotNull(data, "propertyValue_executionIsolationThreadTimeoutInMilliseconds");
        assertNotNull(data, "propertyValue_executionIsolationThreadInterruptOnTimeout");
        // assertNotNull(data,"propertyValue_executionIsolationThreadPoolKeyOverride");
        assertNotNull(data, "propertyValue_executionIsolationSemaphoreMaxConcurrentRequests");
        assertNotNull(data, "propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests");
        assertNotNull(data, "propertyValue_requestCacheEnabled");
        assertNotNull(data, "propertyValue_requestLogEnabled");
        assertNotNull(data, "propertyValue_metricsRollingStatisticalWindowInMilliseconds");
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
            preProcessData(data);
        } catch (err) {
            log("Failed preProcessData: " + err.message);
            return;
        }

        // add the 'addCommas' function to the 'data' object so the HTML templates can use it
        data.addCommas = addCommas;
        // add the 'roundNumber' function to the 'data' object so the HTML templates can use it
        data.roundNumber = roundNumber;
        // add the 'getInstanceAverage' function to the 'data' object so the HTML templates can use it
        data.getInstanceAverage = getInstanceAverage;

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
            var html = tmpl(hystrixTemplateCircuitContainer, data);
            // remove the loading thing first
            $('#' + containerId + ' span.loading').remove();
            // now create the new data and add it
            $('#' + containerId + '').append(html);

            // add the default sparkline graph
            d3.selectAll('#graph_CIRCUIT_' + data.escapedName + ' svg').append("svg:path");

            // remember this is new so we can trigger a sort after setting data
            addNew = true;
        }


        // now update/insert the data
        $('#CIRCUIT_' + data.escapedName + ' div.monitor_data').html(tmpl(hystrixTemplateCircuit, data));

        var ratePerSecond = data.ratePerSecond;
        var ratePerSecondPerHost = data.ratePerSecondPerHost;
        var ratePerSecondPerHostDisplay = ratePerSecondPerHost;
        var errorThenVolume = isNaN(ratePerSecond) ? -1 : (data.errorPercentage * 100000000) + ratePerSecond;
        // set the rates on the div element so it's available for sorting
        $('#CIRCUIT_' + data.escapedName).attr('rate_value', ratePerSecond);
        $('#CIRCUIT_' + data.escapedName).attr('error_then_volume', errorThenVolume);

        // update errorPercentage color on page
        $('#CIRCUIT_' + data.escapedName + ' a.errorPercentage').css('color', this.circuitErrorPercentageColorRange(data.errorPercentage));

        updateCircle('circuit', '#CIRCUIT_' + data.escapedName + ' circle', ratePerSecondPerHostDisplay, data.errorPercentage);

        if (data.graphValues) {
            // we have a set of values to initialize with
            updateSparkline('circuit', '#CIRCUIT_' + data.escapedName + ' path', data.graphValues);
        } else {
            updateSparkline('circuit', '#CIRCUIT_' + data.escapedName + ' path', ratePerSecond);
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
        if (parseInt(newXaxisForCircle) > parseInt(maxXaxisForCircle)) {
            newXaxisForCircle = maxXaxisForCircle;
        }
        var newYaxisForCircle = this[variablePrefix + 'CircleYaxis'](rate);
        if (parseInt(newYaxisForCircle) > parseInt(maxYaxisForCircle)) {
            newYaxisForCircle = maxYaxisForCircle;
        }
        var newRadiusForCircle = this[variablePrefix + 'CircleRadius'](rate);
        if (parseInt(newRadiusForCircle) > parseInt(maxRadiusForCircle)) {
            newRadiusForCircle = maxRadiusForCircle;
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

        sparkline = d3.svg.line()
            // assign the X function to plot our line as we wish
            .x(function (d, i) {
                // return the X coordinate where we want to plot this datapoint based on the time
                return xScale(new Date(d.t));
            })
            .y(function (d) {
                return yScale(d.v);
            })
            .interpolate("basis");

        d3.selectAll(cssTarget).attr("d", sparkline(data));
    }

    /* private */
    deleteCircuit = (circuitName) => {
        $('#CIRCUIT_' + circuitName).remove();
    }
}