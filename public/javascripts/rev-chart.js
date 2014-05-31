/** Inspired by http://bl.ocks.org/stepheneb/1182434. */
RevChart = function(elemid, options, data) {
  var self = this;
  this.chart = document.getElementById(elemid);
  this.cx = this.chart.clientWidth;
  this.cy = this.chart.clientHeight;
  this.options = options || {};
  this.options.key = this.options.key || "metric"
  this.options.label = this.options.label || "Metric value"
  this.data = data || [];

  this.padding = {
    "top": 40,
    "right": 80,
    "bottom": 60,
    "left": 70
  };

  this.size = {
    "width":  this.cx - this.padding.left - this.padding.right,
    "height": this.cy - this.padding.top  - this.padding.bottom
  };

  this.parseDate = d3.time.format("%d-%m-%Y %H:%M:%S").parse;

  this.data.forEach(function(d) {
    d.time = self.parseDate(d.time);
    d[self.options.key] = +d[self.options.key];
  });


  this.color = d3.scale.category10();
  this.color.domain(d3.keys(this.data[0]).filter(function(key) {
    return key.indexOf("metric--") == 0;
  }));

  var xExtent = d3.extent(data, function(d) { return d.time; });
  var xDiff = xExtent[1].getTime() - xExtent[0].getTime();
  xExtent[0] = new Date(xExtent[0].getTime() - 0.01 * xDiff);
  xExtent[1] = new Date(xExtent[1].getTime() + 0.01 * xDiff);
  // x-scale
  this.x = d3.time.scale()
      .domain(xExtent)
      .range([0, this.size.width]);

  // drag x-axis logic
  this.downx = Math.NaN;

  var yExtent = d3.extent(data, function(d) { return d[self.options.key]; });
  var yDiff = yExtent[1] - yExtent[0];
  yExtent[0] = yExtent[0] - 0.03 * yDiff;
  yExtent[1] = yExtent[1] + 0.03 * yDiff;
  // y-scale (inverted domain)
  this.y = d3.scale.linear()
      .domain(yExtent)
      .range([this.size.height, 0]);

  this.selected = this.data[0];

  this.line = d3.svg.line()
//      .interpolate("basis")
      .x(function(d, i) { return self.x(self.data[i].time); })
      .y(function(d, i) { return self.y(self.data[i][self.options.key]); });

  this.vis = d3.select(this.chart).append("svg")
      .attr("width",  this.cx)
      .attr("height", this.cy)
      .append("g")
      .attr("transform", "translate(" + this.padding.left + "," + this.padding.top + ")");

  this.plot = this.vis.append("rect")
      .attr("width", this.size.width)
      .attr("height", this.size.height)
      .style("fill", "#EEEEEE")
      .attr("pointer-events", "all")
      .on("mousedown.drag", self.plot_drag())
      .on("touchstart.drag", self.plot_drag())
  this.plot.call(d3.behavior.zoom().x(this.x).y(this.y).on("zoom", this.redraw()));

  this.vis.append("svg")
      .attr("top", 0)
      .attr("left", 0)
      .attr("width", this.size.width)
      .attr("height", this.size.height)
      .attr("viewBox", "0 0 "+this.size.width+" "+this.size.height)
      .attr("class", "line")
      .append("path")
      .attr("class", "line")
      .attr("d", this.line(this.data));

  // add Chart Title
  this.vis.append("text")
      .attr("class", "axis")
      .text("Revisions")
      .attr("x", this.size.width/2)
      .attr("dy","-0.8em")
      .style("text-anchor","middle");

  // Add the x-axis label
  if (this.options.xlabel) {
    this.vis.append("text")
        .attr("class", "axis")
        .text(this.options.xlabel)
        .attr("x", this.size.width/2)
        .attr("y", this.size.height)
        .attr("dy","2.4em")
        .style("text-anchor","middle");
  }

  this.vis.append("g").append("text")
      .attr("class", "axis")
      .text(this.options.label)
      .style("text-anchor","middle")
      .attr("transform","translate(" + -40 + " " + this.size.height/2+") rotate(-90)");

  d3.select(this.chart)
      .on("mousemove.drag", self.mousemove())
      .on("touchmove.drag", self.mousemove())
      .on("mouseup.drag",   self.mouseup())
      .on("touchend.drag",  self.mouseup());

  this.redraw()();
};

RevChart.prototype.plot_drag = function() {
  var self = this;
  return function() {
    d3.select('body').style("cursor", "move");
  }
};

RevChart.prototype.update = function() {
  var self = this;
  var lines = this.vis.select("path").attr("d", this.line(this.data));

  var circle = this.vis.select("svg").selectAll("circle")
      .data(this.data, function(d) { return d.id; });

  circle.enter().append("circle")
      .attr("class", function(d) { return d === self.selected ? "selected" : null; })
      .attr("cx",    function(d, i) { return self.x(self.data[i].time); })
      .attr("cy",    function(d, i) { return self.y(self.data[i][self.options.key]); })
      .attr("r", 3.0)
      .on("mousedown.drag",  self.datapoint_drag())
      .on("touchstart.drag", self.datapoint_drag());

  circle
      .attr("class", function(d) { return d === self.selected ? "selected" : null; })
      .attr("cx",    function(d, i) { return self.x(self.data[i].time); })
      .attr("cy",    function(d, i) { return self.y(self.data[i][self.options.key]); });

  circle.exit().remove();

  if (d3.event && d3.event.keyCode) {
    d3.event.preventDefault();
    d3.event.stopPropagation();
  }
};

RevChart.prototype.datapoint_drag = function() {
  var self = this;
  return function(d) {
    document.onselectstart = function() { return false; };
    self.selected = d;
    $(self.chart).trigger("revchange", [d.id, d]);
    self.update();
  }
};

RevChart.prototype.mousemove = function() {
  var self = this;
  return function() {
    var p = d3.mouse(self.vis[0][0]);

    if (!isNaN(self.downx)) {
      d3.select('body').style("cursor", "ew-resize");
      var rupx = self.x.invert(p[0]),
          xaxis1 = self.x.domain()[0],
          xaxis2 = self.x.domain()[1],
          xextent = xaxis2 - xaxis1;
      if (rupx != 0) {
        var changex, new_domain;
        changex = self.downx / rupx;
        if (changex > 1) {
          changex *= 1.003;
        } else {
          changex *= 0.997;
        }
        var newxaxis2 = new Date(xaxis1.getTime() + (xextent * changex));
        new_domain = [xaxis1, newxaxis2];
        self.x.domain(new_domain);
        self.redraw()();
      }
      d3.event.preventDefault();
      d3.event.stopPropagation();
    };
  }
};

RevChart.prototype.mouseup = function() {
  var self = this;
  return function() {
    document.onselectstart = function() { return true; };
    d3.select('body').style("cursor", "auto");
    d3.select('body').style("cursor", "auto");
    if (!isNaN(self.downx)) {
      self.redraw()();
      self.downx = Math.NaN;
      d3.event.preventDefault();
      d3.event.stopPropagation();
    };
  }
};

RevChart.prototype.redraw = function() {
  var self = this;
  return function() {
    var tx = function(d) {
          return "translate(" + self.x(d) + ",0)";
        },
        ty = function(d) {
          return "translate(0," + self.y(d) + ")";
        },
        stroke = function(d) {
          return d ? "#ccc" : "#666";
        },
        fx = self.x.tickFormat(10),
        fy = self.y.tickFormat(10);

    // Regenerate x-ticks…
    var gx = self.vis.selectAll("g.x")
        .data(self.x.ticks(10), String)
        .attr("transform", tx);

    gx.select("text")
        .text(fx);

    var gxe = gx.enter().insert("g", "a")
        .attr("class", "x")
        .attr("transform", tx);

    gxe.append("line")
        .attr("stroke", stroke)
        .attr("y1", 0)
        .attr("y2", self.size.height);

    gxe.append("text")
        .attr("class", "axis")
        .attr("y", self.size.height)
        .attr("dy", "1em")
        .attr("text-anchor", "middle")
        .text(fx)
        .style("cursor", "ew-resize")
        .on("mouseover", function(d) { d3.select(this).style("font-weight", "bold");})
        .on("mouseout",  function(d) { d3.select(this).style("font-weight", "normal");})
        .on("mousedown.drag",  self.xaxis_drag())
        .on("touchstart.drag", self.xaxis_drag());

    gx.exit().remove();

    // Regenerate y-ticks…
    var gy = self.vis.selectAll("g.y")
        .data(self.y.ticks(10), String)
        .attr("transform", ty);

    gy.select("text")
        .text(fy);

    var gye = gy.enter().insert("g", "a")
        .attr("class", "y")
        .attr("transform", ty)
        .attr("background-fill", "#FFEEB6");

    gye.append("line")
        .attr("stroke", stroke)
        .attr("x1", 0)
        .attr("x2", self.size.width);

    gye.append("text")
        .attr("class", "axis")
        .attr("x", -3)
        .attr("dy", ".35em")
        .attr("text-anchor", "end")
        .text(fy);

    gy.exit().remove();
    self.plot.call(d3.behavior.zoom().x(self.x).y(self.y).on("zoom", self.redraw()));
    self.update();
  }
};

RevChart.prototype.xaxis_drag = function() {
  var self = this;
  return function(d) {
    document.onselectstart = function() { return false; };
    var p = d3.mouse(self.vis[0][0]);
    self.downx = self.x.invert(p[0]);
  }
};
