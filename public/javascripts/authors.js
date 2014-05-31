var getAuthors = function(callback, opt_rev) {
  $.getJSON("committers.json?rev=" + (opt_rev || ""), callback)
};

var renderAuthors = function() {
  getAuthors(function initialCreateStructure(authors) {
    var $authors = $('#authors');
    for (var i = 0; i < authors.length; i++) {
      var authorElem = createAuthorElem(authors[i]);
      $authors.append(authorElem)
    }
    setActiveAuthors();
  });
  $("#revGraph").on("revchange", function authorsRevChangeHandler(e, sha) {
    setActiveAuthors(sha);
  });
};

var createAuthorElem = function(authorMap) {
  var name = authorMap['name'];
  var id = getAuthorId(name);
  return $('<li/>').attr("id", "author_" + id).addClass("author").text(name);
};

var setActiveAuthors = function(opt_sha) {
  $(".author").removeClass("active");
  getAuthors(function setActive(authors) {
    for (var i = 0; i < authors.length; i++) {
      var name = authors[i]['name'];
      $("#author_" + getAuthorId(name)).addClass("active");
    }
  }, opt_sha);
};

var getAuthorId = function(name) {
  return name.replace(new RegExp('[ <>@\.]', 'g'), "");;
};

$(function() {
  renderAuthors();
});
