var polar = (function() {

    var w = 500;
    var h = 500;
    var r = Math.min(w,h) / 1;
    var s = 0.09;

    var startingData = [{name: 'm', index: 0.1, value: 0},
                        {name: 'm/s', index: 0.2, value: 0},
                        {name: 's/m', index: 0.3, value: 0}];

    var svg = null;
    var fill = d3.scale.linear()
        .range(["#f00", "#0f0", "#ff0"]);
    var arc = d3.svg.arc()
        .startAngle(0)
        .endAngle(function(d) { return d.value * 2 * Math.PI; })
        .innerRadius(function(d) { return d.index * r; })
        .outerRadius(function(d) { return (d.index + s) * r; });

    var init = function() {
        svg = d3.select("#viz").append("svg")
            .attr("width", w)
            .attr("height", h)
            .append("g")
            .attr("transform", "translate(" + w / 2 + "," + h / 2 + ")");

        var g = svg.selectAll("g")
            .data(startingData, function(d) { return d.index; })
            .enter()
            .append("g");

        g.append("path")
            .style("fill", function(d) { return fill(d.value); })
            .attr("d", arc);

        g.append("text")
            .attr("text-anchor", "middle")
            .attr("dy", "1em")
            .text(function(d) { return d.name; });
    }

    var update = function(data) {
        var g = svg.selectAll("g")
            .data(data);

        g.select("path")
            .style("fill", function(d) { return fill(d.value); })
            .attr("d", arc);

        g.select("text")
            .attr("dy", function(d) { return d.value < .5 ? "-.5em" : "1em"; })
            .attr("transform", function(d) {
                return "rotate(" + 360 * d.value + ")"
                    + "translate(0," + -(d.index + s / 2) * r + ")"
                    + "rotate(" + (d.value < .5 ? -90 : 90) + ")"
            })
            .text(function(d) { return d.name; });
    }

    var reset = function() {
        update(startingData);
    }

    return {
        init: init,
        update: update,
        reset: reset
    }

})();
