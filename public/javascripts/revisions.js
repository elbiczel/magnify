var getRevisions = function(callback) {
  $.getJSON("revisions.json?details", callback)
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

$(function() {
  getRevisions(drawGraph);
});

function drawGraph(revisions) {
  var chart = new RevChart("revGraph", { key: "metric--loc", label: "Lines of Code" }, revisions)
}
