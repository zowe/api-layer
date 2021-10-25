/* eslint-disable */
/*
This file is a modified version of monitor.html.
The changes are largely simply taking the monitor.html script elements and providing them in this Javascript file.
*/

//Read a page's GET URL variables and return them as an associative array.
// from: http://jquery-howto.blogspot.com/2009/09/get-url-parameters-values-with-jquery.html
function getUrlVars() {
    var vars = [], hash;
    const url = 'https://localhost:10012/discoverableclient/hystrix/monitor?stream=https%3A%2F%2Flocalhost%3A10012%2Fdiscoverableclient%2Fapplication%2Fhystrix.stream&title=Discoverable%20Client%20Service';
    var hashes = url.slice(url.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++) {
        hash = hashes[i].split('=');
        vars.push(hash[0]);
        vars[hash[0]] = hash[1];
    }
    return vars;
}

var hystrixStreams = [];

function addStreams(proxyStream) {
    var urlVars = getUrlVars();

    var streams = urlVars.streams ? JSON.parse(decodeURIComponent(urlVars.streams)) :
        urlVars.stream ? [{
            stream: decodeURIComponent(urlVars.stream),
            delay: urlVars.delay,
            name: decodeURIComponent(urlVars.title),
            auth: urlVars.authorization
    }] : [];

    _.map(streams, function(s, i) {
        var dependenciesId = 'dependencies_' + i;

        var hystrixMonitor = new HystrixCommandMonitor(i, dependenciesId, {includeDetailIcon:false});
        var dependencyThreadPoolMonitor = new HystrixThreadPoolMonitor(i, 'dependencyThreadPools_' + i);

        hystrixStreams[i] = {
            titleName: s.name || s.stream,
            hystrixMonitor: hystrixMonitor,
            dependencyThreadPoolMonitor: dependencyThreadPoolMonitor
        };

        // sort by error+volume by default
        hystrixMonitor.sortByErrorThenVolume();
        dependencyThreadPoolMonitor.sortByVolume();

        var origin;
        if(s != undefined) {
            origin = s.stream;

            if(s.delay) {
                origin = origin + "&delay=" + s.delay;
            }

            //do not show authorization in stream title
            if(s.auth) {
                origin = origin + "&authorization=" + s.auth;
            }
        }

        // start the EventSource which will open a streaming connection to the server
        var source = new EventSource(proxyStream);

        // add the listener that will process incoming events
        source.addEventListener('message', hystrixMonitor.eventSourceMessageListener, false);
        source.addEventListener('message', dependencyThreadPoolMonitor.eventSourceMessageListener, false);

        //	source.addEventListener('open', function(e) {
        //		console.console.log(">>> opened connection, phase: " + e.eventPhase);
        //		// Connection was opened.
        //	}, false);

        source.addEventListener('error', function(e) {
            $("#" + dependenciesId + " .loading").html("Unable to connect to Command Metric Stream.");
            $("#" + dependenciesId + " .loading").addClass("failed");
            if (e.eventPhase == EventSource.CLOSED) {
                // Connection was closed.
                console.log("Connection was closed on error: " + e);
            } else {
                console.log("Error occurred while streaming: " + e);
            }
        }, false);
    });

    addMonitors();
}

function addMonitors() {
    $("#content").html(_.reduce(hystrixStreams, function(html, s, i) {
        var hystrixMonitor = 'hystrixStreams[' + i + '].hystrixMonitor';
        var dependencyThreadPoolMonitor = 'hystrixStreams[' + i + '].dependencyThreadPoolMonitor';
        var dependenciesId = 'dependencies_' + i;
        var dependencyThreadPoolsId = 'dependencyThreadPools_' + i;
        var title_name = 'title_name_' + i;

        return html +
            '<div id="monitor">' +
                '<div id="streamHeader">' +
                    '<h2><span id="' + title_name + '"></span>Hystrix Stream: ' + s.titleName + '</h2>' +
                '</div>' +
                '<div class="monitor-container">' +
                    '<div class="row">' +
                        '<div class="menubar">' +
                            '<div class="title">' +
                                'Circuit' +
                            '</div>' +
                            '<div class="menu_actions">' +
                                'Sort: ' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortByErrorThenVolume();">Error then Volume</a> |' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortAlphabetically();">Alphabetical</a> | ' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortByVolume();">Volume</a> | ' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortByError();">Error</a> | ' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortByLatencyMean();">Mean</a> | ' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortByLatencyMedian();">Median</a> | ' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortByLatency90();">90</a> | ' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortByLatency99();">99</a> | ' +
                                '<a href="javascript://" onclick="' + hystrixMonitor + '.sortByLatency995();">99.5</a> ' +
                            '</div>' +
                            '<div class="menu_legend">' +
                                '<span class="success">Success</span> | <span class="shortCircuited">Short-Circuited</span> | <span class="badRequest"> Bad Request</span> | <span class="timeout">Timeout</span> | <span class="rejected">Rejected</span> | <span class="failure">Failure</span> | <span class="errorPercentage">Error %</span>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                    '<div id="' + dependenciesId + '" class="row dependencies"><span class="loading">Loading ...</span></div>' +
                    '<div class="spacer"></div>' +

                    '<div class="row">' +
                        '<div class="menubar">' +
                            '<div class="title">' +
                                'Thread Pools' +
                            '</div>' +
                            '<div class="menu_actions">' +
                                'Sort: <a href="javascript://" onclick="' + dependencyThreadPoolMonitor + '.sortAlphabetically();">Alphabetical</a> | ' +
                                '<a href="javascript://" onclick="' + dependencyThreadPoolMonitor + '.sortByVolume();">Volume</a> | ' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                    '<div id="' + dependencyThreadPoolsId + '" class="row dependencyThreadPools"><span class="loading">Loading ...</span></div>' +
                    '<div class="spacer"></div>' +
                    '<div class="spacer"></div>' +
                '</div>' +
            '</div>';
    }, ''));
}
