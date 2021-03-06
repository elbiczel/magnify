var getRevisions = function(callback) {
  $.getJSON("revisions.json?details", callback)
};

RevDetails = function(elemid) {
  this.elem = $("#" + elemid);
  this.shaElem = $('<span/>').addClass("sha");
  this.authorElem = $('<span/>').addClass("rev_author");
  this.committerElem = $('<span/>').addClass("rev_committer");
  this.descElem = $('<span/>').addClass("desc");
  this.timeElem = $('<span/>').addClass("time");
  this.elem.addClass("details").addClass("revision").
      append(this.shaElem).append(" ")
      .append(this.authorElem).append(" @ ")
      .append(this.timeElem).append($('<br/>'))
      .append("commited by: ").append(this.committerElem).append($('<br/>'))
      .append(this.descElem);
};

RevDetails.prototype.setRev = function(rev) {
  var sha = rev['id'];
  var author = rev['author'];
  var committer = rev['committer'];
  var desc = rev['description'];
  var time = rev['time'];
  this.shaElem.text(sha);
  this.authorElem.text(author);
  this.committerElem.text(committer);
  this.descElem.text(desc);
  this.timeElem.text(time);
};
