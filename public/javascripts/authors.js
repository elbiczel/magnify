/** Based on: http://bl.ocks.org/dbuezas/9572040 */
AuthorChart = function(elemid, opt_key, opt_authorColor) {
  var self = this;
  this.duration = 300;
  this.elem = $("#" + elemid);
  this.parent = d3.select("#" + elemid)
      .append("svg")
  this.svg = this.parent
      .append("g");
  this.setKey(opt_key);

  this.svg.append("g")
      .attr("class", "slices");
  this.svg.append("g")
      .attr("class", "labels");
  this.svg.append("g")
      .attr("class", "lines");

  this.width = this.elem.width();
  this.height = this.elem.height() - $("form", this.elem).outerHeight(true); // padding
  this.radius = Math.min(this.width, this.height) / 2 - 150;

  this.title = this.parent.append("text")
      .attr("class", "axis")
      .attr("x", this.width/2)
      .attr("y", $("form", this.elem).outerHeight(true))
      .attr("dy","-0.8em")
      .style("text-anchor","middle");

  this.pie = d3.layout.pie()
      .sort(null)
      .value(function(d) { return d.value; });

  this.arc = d3.svg.arc()
      .outerRadius(this.radius * 0.8)
      .innerRadius(this.radius * 0.4);

  this.outerArc = d3.svg.arc()
      .innerRadius(this.radius * 0.9)
      .outerRadius(this.radius * 0.9);

  this.svg.attr("transform", "translate("+this.width/2+","+(this.radius+30)+")");

  this.keyFn = function(d) { return d.data.label; };

  var subColor = opt_authorColor || d3.scale.category20();
  this.color = function(label) {
    if (label === "None" || label === "Other") {
      return "#111";
    }
    return subColor(label);
  };
  d3.selectAll("#" + elemid + " input")
      .on("change", function() { self.setKey(this.value); });
};

AuthorChart.prototype.setObj = function(obj) {
  if (!obj) return;
  var title = ""
  if (obj.id) {
    title = "revision: " + obj.id;
  } else {
    title = [obj.kind, obj.name].join(": ")
  }
  this.title.text(title);
  this.obj = obj;
  var self = this;
  var keys = d3.keys(this.obj).filter(function(key) { return key.indexOf(self.key) == 0; });
  this.data = keys.map(function(key) {
    return {
      label: key.split("---")[1],
      value: +self.obj[key]
    };
  }).sort(function(a,b) {
    return d3.ascending(a.label, b.label);
  });
  if (this.data.length == 0) { this.data = [{label: "None", value: 1}]; } else {
    var sum = d3.sum(this.data, function(o) { return o.value });
    var threshold = sum * 0.05;
    var other = 0;
    var otherCount = 0;
    var newData = this.data.filter(function(o) {
      if (o.value < threshold) {
        other += o.value;
        otherCount++;
        return false;
      }
      return true;
    });
    newData.push({
      label: "Other",
      value: other
    });
    if (otherCount > 1) {
      this.data = newData;
    }
  }
  this.changed_()
};

AuthorChart.prototype.setKey = function(opt_key) {
  this.key = opt_key || "metric";
  this.setObj(this.obj);
};

AuthorChart.prototype.renderSliceArcs_ = function(was, is) {
  var self = this;
  var slice = this.svg.select(".slices").selectAll("path.slice")
      .data(this.pie(was), this.keyFn);

  slice.enter()
      .insert("path")
      .attr("class", "slice")
      .style("fill", function(d) {
        return self.color(d.data.label);
      })
      .each(function(d) {
        this._current = d;
      });

  slice = this.svg.select(".slices").selectAll("path.slice")
      .data(this.pie(is), this.keyFn);

  slice
      .transition().duration(this.duration)
      .attrTween("d", function(d) {
        var interpolate = d3.interpolate(this._current, d);
        var _this = this;
        return function(t) {
          _this._current = interpolate(t);
          return self.arc(_this._current);
        };
      });

  slice = this.svg.select(".slices").selectAll("path.slice")
      .data(this.pie(this.data), this.keyFn);

  slice
      .exit().transition().delay(this.duration).duration(0)
      .remove();
};

AuthorChart.prototype.renderTextLabels_ = function(was, is) {
  var self = this;
  var text = this.svg.select(".labels").selectAll("text")
      .data(this.pie(was), this.keyFn);

  text.enter()
      .append("text")
      .attr("dy", ".35em")
      .style("opacity", 0)
      .text(function(d) {
        return d.data.label;
      })
      .each(function(d) {
        this._current = d;
      });

  text = this.svg.select(".labels").selectAll("text")
      .data(this.pie(is), this.keyFn);

  text.transition().duration(this.duration)
      .style("opacity", function(d) {
        return d.data.value == 0 ? 0 : 1;
      })
      .attrTween("transform", function(d) {
        var interpolate = d3.interpolate(this._current, d);
        var _this = this;
        return function(t) {
          var d2 = interpolate(t);
          _this._current = d2;
          var pos = self.outerArc.centroid(d2);
          pos[0] = self.radius * (midAngle(d2) < Math.PI ? 1 : -1);
          return "translate("+ pos +")";
        };
      })
      .styleTween("text-anchor", function(d){
        var interpolate = d3.interpolate(this._current, d);
        return function(t) {
          var d2 = interpolate(t);
          return midAngle(d2) < Math.PI ? "start":"end";
        };
      });

  text = this.svg.select(".labels").selectAll("text")
      .data(this.pie(this.data), this.keyFn);

  text
      .exit().transition().delay(this.duration)
      .remove();
};

AuthorChart.prototype.renderTextMarkers_ = function(was, is) {
  var self = this;
  var polyline = this.svg.select(".lines").selectAll("polyline")
      .data(this.pie(was), this.keyFn);

  polyline.enter()
      .append("polyline")
      .style("opacity", 0)
      .each(function(d) {
        this._current = d;
      });

  polyline = this.svg.select(".lines").selectAll("polyline")
      .data(this.pie(is), this.keyFn);

  polyline.transition().duration(this.duration)
      .style("opacity", function(d) {
        return d.data.value == 0 ? 0 : .5;
      })
      .attrTween("points", function(d){
        this._current = this._current;
        var interpolate = d3.interpolate(this._current, d);
        var _this = this;
        return function(t) {
          var d2 = interpolate(t);
          _this._current = d2;
          var pos = self.outerArc.centroid(d2);
          pos[0] = self.radius * 0.95 * (midAngle(d2) < Math.PI ? 1 : -1);
          return [self.arc.centroid(d2), self.outerArc.centroid(d2), pos];
        };
      });

  polyline = this.svg.select(".lines").selectAll("polyline")
      .data(this.pie(this.data), this.keyFn);

  polyline
      .exit().transition().delay(this.duration)
      .remove();
};

AuthorChart.prototype.changed_ = function() {
  var data0 = this.svg.select(".slices").selectAll("path.slice")
      .data().map(function(d) { return d.data });
  if (data0.length == 0) data0 = this.data;
  var was = mergeWithFirstEqualZero(this.data, data0);
  var is = mergeWithFirstEqualZero(data0, this.data);
  this.renderSliceArcs_(was, is);
  this.renderTextLabels_(was, is);
  this.renderTextMarkers_(was, is);
};

function mergeWithFirstEqualZero(first, second) {
  var secondSet = d3.set(); second.forEach(function(d) { secondSet.add(d.label); });

  var onlyFirst = first
      .filter(function(d){ return !secondSet.has(d.label) })
      .map(function(d) { return {label: d.label, value: 0}; });
  return d3.merge([ second, onlyFirst ])
      .sort(function(a,b) {
        return d3.ascending(a.label, b.label);
      });
};

function midAngle(d) {
  return d.startAngle + (d.endAngle - d.startAngle) / 2;
};
