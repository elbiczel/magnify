var getRevisions = function(callback) {
  $.getJSON("revisions.json?details", callback)
};

var renderRevisions = function() {
  getRevisions(function initialCreateRevStructure(revisions) {
    var $revisions = $('#revisions');
    for (var i = 0; i < revisions.length; i++) {
      var classes = [];
      if (i == 0) {
        classes.push("active");
      }
      var revElem = createRevElement(revisions[i], classes, $revisions);
      $revisions.append(revElem)
    }
    drawGraph(revisions)
  });
};

var createRevElement = function(revMap, classes, $revisions) {
  var sha = revMap['id'];
  var author = revMap['author'];
  var committer = revMap['committer'];
  var desc = revMap['description'];
  var time = revMap['time'];
  var shaElem = $('<span/>').addClass("sha").text(sha);
  var authorElem = $('<span/>').addClass("rev_author").text(author);
  var committerElem = $('<span/>').addClass("rev_committer").text(committer);
  var descElem = $('<span/>').addClass("desc").text(desc);
  var timeElem = $('<span/>').addClass("time").text(time);
  var detailsElem = $('<div/>').addClass("details").addClass("hide")
      .append(shaElem).append(" ").append(authorElem).append(" @ ").append(timeElem).append($('<br/>'))
      .append("commited by: ").append(committerElem).append($('<br/>'))
      .append(descElem);
  var elem = $('<li/>').attr("id", "rev_" + sha).addClass("revision")
      .text(sha)
      .append(detailsElem);
  elem.dblclick(function setActive() {
    var $this = $(this);
    var $active = $(".revision.active");
    if ($this.attr("id") !== $active.attr("id")) {
      $active.removeClass("active");
      $this.addClass("active");
      $revisions.trigger("revchange")
    }
  });
  elem.popover({
    html: true,
    container: 'body',
    content: function getContent() {
      return $(".details", this).html();
    }
  });
  for (var i = 0; i < classes.length; i++) {
    elem = elem.addClass(classes[i]);
  }
  return elem
};

var getActiveSha = function() {
  var activeElemId = $('.revision.active').attr("id");
  return (activeElemId && activeElemId.split("_")[1]) || "";
};

$(function() {
  renderRevisions();
});

function drawGraph(revisions) {
  var $revGraph = $("#revGraph");
  var margin = {top: 20, right: 80, bottom: 30, left: 50};
  var width = $revGraph.width() - margin.left - margin.right;
  var height = $revGraph.height() - margin.top - margin.bottom;
  var parseDate = d3.time.format("%d-%m-%Y %H:%M:%S").parse;
  var x = d3.time.scale()
      .range([0, width]);
  var y = d3.scale.linear()
      .range([height, 0]);
  var color = d3.scale.category10();
  var xAxis = d3.svg.axis()
      .scale(x)
      .orient("bottom");
  var yAxis = d3.svg.axis()
      .scale(y)
      .orient("left");
  var line = d3.svg.line()
      .interpolate("basis")
      .x(function(d) { return x(d.time); })
      .y(function(d) { return y(d.metric); });
  var svg = d3.select("#revGraph").append("svg")
      .attr("width", width + margin.left + margin.right)
      .attr("height", height + margin.top + margin.bottom)
      .append("g")
      .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
  revisions.forEach(function(d) {
    d.time = parseDate(d.time);
    d.metric = +d["metric--loc"]
  });

  color.domain(d3.keys(revisions[0]).filter(function(key) {
    return key.indexOf("metric--") == 0;
  }));
  x.domain(d3.extent(revisions, function(d) { return d.time; }));
  y.domain(d3.extent(revisions, function(d) { return d.metric; }));

  svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height + ")")
      .call(xAxis);
  svg.append("g")
      .attr("class", "y axis")
      .call(yAxis)
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Lines of Code");

  svg.append("path")
      .datum(revisions)
      .attr("class", "line")
      .attr("d", line)
      .style("stroke", function(d) { return color("metric--loc"); });
}
