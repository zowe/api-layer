/* eslint-disable */
window.HystrixCircuitContainerTemplate = (props) => {
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

    return `<div class="monitor" id="${`CIRCUIT_${`${name}_${index}`}`}" style="position:relative;">
                <div
                    id="${`chart_CIRCUIT_${`${name}_${index}`}`}"
                    class="chart"
                    style="position:absolute;top:0px;left:0; float:left; width:100%; height:100%;"
                />
                <div
                    style="position:absolute;top:0;width:100%;height:15px;opacity:0.8; background:white;"
                >
                    <p class="name" />
                </div>
                <div
                    style="position:absolute;top:15px;; opacity:0.8; background:white; width:100%; height:95%;"
                >
                    <div class="monitor_data" />
                </div>
                <div
                    id="${`graph_CIRCUIT_${`${name}_${index}`}`}"
                    class="graph"
                    style="position:absolute;top:25px;left:0; float:left; width:140px; height:62px;"
                />
                <script>
                    var y = 200;
                    /* escape with two backslashes */
                    var vis = d3.select("#chart_CIRCUIT_${name.replace(/([ !"#$%&'()*+,./:;<=>?@[\]^`{|}~])/g,'\\\\$1') + '_' + index }").append("svg:svg").attr("width", "100%").attr("height", "100%");
                    /* add a circle -- we don't use the data point, we set it manually, so just passing in [1] */
                    var circle = vis.selectAll("circle").data([1]).enter().append("svg:circle");
                    /* setup the initial styling and sizing of the circle */
                    circle.style("fill", "green").attr("cx", "30%").attr("cy", "30%").attr("r", 5);
                    
                    /* add the line graph - it will be populated by javascript, no default to show here */
                    /* escape with two backslashes */
                    var graph = d3.select("#graph_CIRCUIT_${name.replace(/([ !"#$%&'()*+,./:;<=>?@[\]^`{|}~])/g,'\\\\$1') + '_' + index }").append("svg:svg").attr("width", "100%").attr("height", "100%");
                </script>
            </div>`;
}
