createAuthorChart = (authorColors) ->
  authorChart = new AuthorChart("authors", "metric--aggr-cont", authorColors);
  lastRev = null;
  $("#revGraph").on "revchange", (event, sha, rev) ->
    lastRev = rev
    authorChart.setObj(rev)
  $("#chart").on "objselect", (event, obj) ->
    authorChart.setObj(obj || lastRev)

createRevChart = (authorColors) ->
  getRevisions (revisions) ->
    authorColors.domain(d3.set(d3.keys(revisions[0]).filter (key) -> key.split("---")[1]).values())
    revDetails = new RevDetails("revDetails");
    $("#revGraph").on "revchange", (event, sha, rev) ->
      revDetails.setRev(rev)
    chart = new RevChart(
      "revGraph",
      { key: "metric--loc", label: "Lines of Code", authorColor: authorColors },
      revisions);

$ ->
  authorColors = d3.scale.category20();
  createAuthorChart(authorColors)
  createRevChart(authorColors)
  lastSha = ""
  jsonAddress = (address, opt_sha) ->
    if opt_sha
      lastSha = opt_sha
    address + "?rev=" + (opt_sha || lastSha)

  currentCreateFunction = (opt_sha) ->
    makeSvg(jsonAddress("packages.json", opt_sha))

  badness = d3.scale.linear().domain([-1, 300]).range(["green", "red"])

  defaultNodeColor =
    "package": () -> "#000"
    "class": () -> "#555"
  kindNodeColor = (node) -> defaultNodeColor[node.kind](node)
  metricNodeColor = (metric) -> (node) -> badness(node["metric--" + metric])
  avgLocColor = metricNodeColor("avg-loc")
  authorBasedColor = (node) ->
    owner = node["metric--owner"]
    if owner
      authorColors(owner)
    else
      "#000"
  color = null

  defaultStrengths =
    "in-package": () -> 1
    "pkg-imports-pkg": () -> 0.1
    "cls-imports-cls": (link) -> if link.source["parent-pkg-name"] == link.target["parent-pkg-name"] then 0.2 else 0
    calls: () -> 0
    "cls-in-pkg": () -> 1
    "cls-imports-pkg": () -> 0
    "pkg-imports-cls": () -> 0
  strengths = null
  strength = (link) -> strengths[link.kind](link)

  defaultLinkColors =
    "in-package": () -> "#cc0000"
    "pkg-imports-pkg": () -> "#babdb6"
    "cls-imports-cls": () -> "#d3d7df"
    calls: () -> "#fce94f"
    "cls-in-pkg": () -> "transparent"
    "cls-imports-pkg": () -> "#d3d7df"
    "pkg-imports-cls": () -> "#d3d7df"
  linkColors = null
  linkColor = (link) -> linkColors[link.kind](link)

  defaultLinkWidths =
    "in-package": (link) -> if !link.source.expanded and !link.target.expanded then 1.5 else 0.1
    "pkg-imports-pkg": (link) ->
      if !link.source.expanded and !link.target.expanded
        Math.min((Math.log(link.weight) / 3) + 1.5, 10)
      else
        0
    "cls-imports-cls": (link) -> if link.source.visible and link.target.visible then 1 else 0
    calls: (link) -> Math.min(link.count / 10.0, 5)
    "cls-in-pkg": () -> 0
    "cls-imports-pkg": (link) -> if link.source.visible and !link.target.expanded then 1 else 0
    "pkg-imports-cls": (link) -> if !link.source.expanded and link.target.visible then 1 else 0
  linkWidths = null
  linkWidth = (link) ->
    if !linkWidth.withSelected or link.source.selected or link.target.selected
      linkWidths[link.kind](link)
    else
      0

  defaultLinkDistances =
    "in-package": () -> 60
    "pkg-imports-pkg": () -> 360
    "cls-imports-cls": (link) -> if link.source["parent-pkg-name"] == link.target["parent-pkg-name"] then 10 else 360
    calls: () -> 360
    "cls-in-pkg": () -> 10
    "cls-imports-pkg": () -> 360
    "pkg-imports-cls": () -> 360
  linkDistances = null
  linkDistance = (link) -> linkDistances[link.kind](link)

  defaultNodeSize =
    "package": (node) -> if !node.expanded then 5 else 1e-6
    "class": (node) -> if node.visible then 3 else 0
  kindNodeSize = (node) -> defaultNodeSize[node.kind](node)
  distinctAuthorsNodeSize = (node) ->
    if node.kind == "package" and !node.expanded
      10 + 2 * node["metric--dist-auth"]
    else if node.kind == "class" and node.visible
      3 + 2 * node["metric--dist-auth"]
    else
      0
  metricNodeSize = (metric) -> (node) ->
    if (node.kind == "package")
      if !node.expanded then 3 + Math.max(3, 100.0 * node["metric--" + metric]) else 1e-6
    else if (node.kind == "class")
      if node.visible then 3 + Math.max(3, 100.0 * node["metric--" + metric]) else 0
  pageRankNodeSize = metricNodeSize("pr")
  nodeR = null
  nodeSize = (node) ->
    if !nodeSize.withSelected or node.selected or node.neighbour
      nodeR(node)
    else
      0


  defaultMetrics = () ->
    color = authorBasedColor
    strengths =
      "in-package": defaultStrengths["in-package"]
      "pkg-imports-pkg": defaultStrengths["pkg-imports-pkg"]
      "cls-imports-cls": defaultStrengths["cls-imports-cls"]
      calls: defaultStrengths.calls
      "cls-in-pkg": defaultStrengths["cls-in-pkg"]
      "cls-imports-pkg": defaultStrengths["cls-imports-pkg"]
      "pkg-imports-cls": defaultStrengths["pkg-imports-cls"]
    linkColors =
      "in-package": defaultLinkColors["in-package"]
      "pkg-imports-pkg": defaultLinkColors["pkg-imports-pkg"]
      "cls-imports-cls": defaultLinkColors["cls-imports-cls"]
      calls: defaultLinkColors.calls
      "cls-in-pkg": defaultLinkColors["cls-in-pkg"]
      "cls-imports-pkg": defaultLinkColors["cls-imports-pkg"]
      "pkg-imports-cls": defaultLinkColors["pkg-imports-cls"]
    linkWidths =
      "in-package": defaultLinkWidths["in-package"]
      "pkg-imports-pkg": defaultLinkWidths["pkg-imports-pkg"]
      "cls-imports-cls": defaultLinkWidths["cls-imports-cls"]
      calls: defaultLinkWidths.calls
      "cls-in-pkg": defaultLinkWidths["cls-in-pkg"]
      "cls-imports-pkg": defaultLinkWidths["cls-imports-pkg"]
      "pkg-imports-cls": defaultLinkWidths["pkg-imports-cls"]
    linkDistances =
      "in-package": defaultLinkDistances["in-package"]
      "pkg-imports-pkg": defaultLinkDistances["pkg-imports-pkg"]
      "cls-imports-cls": defaultLinkDistances["cls-imports-cls"]
      calls: defaultLinkDistances.calls
      "cls-in-pkg": defaultLinkDistances["cls-in-pkg"]
      "cls-imports-pkg": defaultLinkDistances["cls-imports-pkg"]
      "pkg-imports-cls": defaultLinkDistances["pkg-imports-cls"]
    nodeR = distinctAuthorsNodeSize
  defaultMetrics()

  $chart = $("#chart")
  width = $chart.width()
  height = $(window).height() * 0.8

  force = d3.layout.force()
    .charge(-60)
    .linkDistance(linkDistance)
    .linkStrength(strength)
    .size([width, height])
    .gravity(0.1)
    .friction(0.9)

  svg = d3
    .select("#chart")
    .append("svg:svg")
    .attr("width", width)
    .attr("height", height)
    .attr("pointer-events", "all")

  scale = 1
  isTitleDisplayable = (opt_min_scale) -> scale > (opt_min_scale || 3)

  titleDisplay = (d) ->
    if titleDisplay.withSelected
      if (d.selected or d.neighbour) and isTitleDisplayable(1.5) and ((d.kind == "package" and !d.expanded) or (d.kind == "class" and d.visible)) then "" else "none"
    else
      if isTitleDisplayable() and ((d.kind == "package" and !d.expanded) or (d.kind == "class" and d.visible))
        ""
      else
        "none"

  vis = svg
    .append("svg:g")
    .call(d3.behavior.zoom().on("zoom", ->
      scale = d3.event.scale
      vis.attr("transform", "translate("+d3.event.translate+")"+" scale("+d3.event.scale+")")
      nodeTitle.transition(50)
        .style("display", titleDisplay)
    )).on("dblclick.zoom", null)
    .append('svg:g')

  vis.append('svg:rect')
    .attr('width', width)
    .attr('height', height)
    .attr('fill', 'transparent');

  svg
    .append("svg:defs")
      .append("svg:marker")
        .attr("id","arrow-head").attr("viewBox", "0 0 10 10")
        .attr("refX", 1).attr("refY", 5)
        .attr("markerWidth", 4).attr("markerHeight", 3)
        .attr("markerUnit", "strokeWidth").attr("orient", "auto")
        .append("svg:polyline")
          .style("fill","#000")
          .attr("points", "0,0 5,5 0,10 1,5")

  link = vis.selectAll("line.link")
  node = vis.selectAll("circle.node")
  nodeTitle = node.selectAll("text")

  force.on "tick", ->
    link
      .attr("x1", (d) -> d.source.x)
      .attr("y1", (d) -> d.source.y)
      .attr("x2", (d) -> d.target.x)
      .attr("y2", (d) -> d.target.y)
    node
      .attr("cx", (d) -> d.x)
      .attr("cy", (d) -> d.y)
    nodeTitle
      .attr("x", (d) -> d.x)
      .attr("y", (d) -> d.y)

  packages = {}
  classesPerPackage = {}

  setExpanded = (d, expanded) ->
    d = if (typeof d) == "string" then packages[d] else d
    d.expanded = expanded
    cls.visible = expanded for cls in (classesPerPackage[d.name] || [])

  dblclick = (d) ->
    if (d == selectedNode)
      deselectNode(d)
    if (d.kind == "package")
      if !classesPerPackage[d.name] or !classesPerPackage[d.name].length then return
      setExpanded(d, true)
    else if (d.kind == "class")
      setExpanded(d["parent-pkg-name"], false)
    node
      .transition()
        .duration(750)
        .attr("r", nodeSize)
        .style("fill", color)
    link
      .transition()
        .duration(750)
          .style("stroke-width", linkWidth)
          .style("stroke", linkColor)
    nodeTitle.transition(250)
      .style("display", titleDisplay)
    force.linkStrength(strength).resume()


  selectedNode = null

  deselectNode = (d) ->
    d.selected = false
    selectedNode = null
    $chart.trigger("objselect", selectedNode)
    linkWidth.withSelected = false
    nodeSize.withSelected = false
    titleDisplay.withSelected = false
    nodeTitle.transition(250)
      .style("display", titleDisplay)

  click = (d) ->
      if d3.event.defaultPrevented then return
      neighbour.neighbour = false for neighbour in force.nodes()
      if d == selectedNode
        deselectNode(d)
      else
        if selectedNode
          selectedNode.selected = false
        d.selected = true
        selectedNode = d
        $chart.trigger("objselect", selectedNode)
        linkWidth.withSelected = true
        nodeSize.withSelected = true
        titleDisplay.withSelected = true
        links = force.links().filter((link) -> link.source.name == d.name or link.target.name == d.name)
        neighbours = links.map((link) -> if link.source.name == d.name then link.target else link.source)
        neighbour.neighbour = true for neighbour in neighbours
      link
        .transition()
          .duration(750)
          .style("stroke-width", linkWidth)
      node
        .transition()
          .duration(750)
          .attr("r", nodeSize)
      nodeTitle.transition(250)
        .style("display", titleDisplay)



  makeSvg = (jsonAddress) ->
    d3.json jsonAddress, (json) ->
      oldByName = {}
      oldByName[n.name] = n for n in force.nodes()
      updateNewNode = (node) ->
        if oldByName[node.name]
          old = oldByName[node.name]
          node.x = old.x
          node.y = old.y
          node.px = old.px
          node.py = old.py
          node.visible = old.visible
          node.expanded = old.expanded
          node.selected = old.selected
          node.neighbour = old.neighbour
          if (node.selected)
            selectedNode = node
            $chart.trigger("objselect", selectedNode)

      updateNewNode node for node in json.nodes
      if selectedNode and !json.nodes.some((node) -> node.selected)
        selectedNode = null
        $chart.trigger("objselect", selectedNode)
        node.neighbour = false for node in json.nodes
        linkWidth.withSelected = false
        nodeSize.withSelected = false
      force
        .nodes(json.nodes)
        .links(json.edges)
        .start()

      packages = {}
      classesPerPackage = {}
      pkgs = json.nodes.filter((d) -> d.kind == "package")
      packages[d.name] = d for d in pkgs
      updateClassPerPackage = (cls) ->
        if !classesPerPackage[cls["parent-pkg-name"]] then classesPerPackage[cls["parent-pkg-name"]] = []
        classesPerPackage[cls["parent-pkg-name"]].push(cls)
      clses = json.nodes.filter((d) -> d.kind == "class")
      updateClassPerPackage(cls) for cls in clses

      keyFunction = (d) -> [d.kind, d.source.name, d.target.name].join(",")
      link = vis.selectAll("line.link")
        .data(json.edges, keyFunction)

      # update
      #EMPTY

      # enter
      enterLink = link.enter()
        .append("svg:line")
        .attr("class", "link")
        #.attr("marker-end", "url(#arrow-head)")
        .style("stroke-width", 1e-6)
        .style("fill-opacity", 1e-6)

      # enter + update
      link
        .style("stroke", linkColor)
        .transition()
          .duration(750)
          .style("stroke-width", linkWidth)
          .style("fill-opacity", 1)

      # exit
      exitLink = link.exit()
        .attr("class", "exit")
        .transition(250)
          .style("stroke-width", 1e-6)
          .style("fill-opacity", 1e-6)
          .remove()

      node = vis.selectAll("circle.node")
        .data(json.nodes, (d) -> d.name)

      # update
      #EMPTY

      # enter
      enterNode = node.enter()
        .append("circle")
        .attr("class", "node")
        .attr("r", 1e-6)
        .style("fill-opacity", 1e-6)
        .on("dblclick", dblclick)
        .on("click", click)
        .call(force.drag)
      enterNode
        .append("title")
        .text((d) -> d.name)
      nodeTitle = vis.selectAll("text")
        .data(json.nodes, (d) -> d.name)
      nodeTitle.enter()
        .append("text")
        .attr("text-anchor", "middle")
        .attr("dy", ".7em")
        .attr("dx", "3em")
        .style("font-size", ".35em")
        .style("display", titleDisplay)
        .text((d) -> d.name)

      # enter + update
      node
        .style("fill", color)
        .transition()
          .duration(750)
          .attr("r", nodeSize)
          .style("fill-opacity", 1)


      # exit
      exitNode = node.exit()
        .attr("class", "exit")
        .transition(750)
          .style("fill-opacity", 1e-6)
          .remove()

  $(".custom-button").on "click", (event) -> if (!$(".nav-graph-custom-tab").hasClass("active"))
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-custom-tab").addClass("active")
    $(".gauges").remove()
    $(".mag-sidenav").after(
      """
      <nav class="navbar navbar-default gauges" role="navigation" id="customNav">
        <div class="container-fluid">
          <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
              <span class="sr-only">Toggle navigation</span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
            </button>
          </div>
          <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
            <ul class="nav navbar-nav">
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Edge<b class="caret"></b></a>
                <ul class="dropdown-menu">
                  <li><label class="checkbox inline"><input type="checkbox" value="" class="check-pkg-imports"/>imports</label></li>
                  <li><label class="checkbox inline"><input type="checkbox" value="" class="check-contains"/>contains</label></li>
                  <li><label class="checkbox inline"><input type="checkbox" value="" class="check-runtime"/>runtime</label></li>
                </ul>
              </li>
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Node size<b class="caret"></b></a>
                <ul class="dropdown-menu">
                  <li><label class="radio"><input type="radio" name="node-size" value="constant" checked="checked"/>Constant</label></li>
                  <li><label class="radio"><input type="radio" name="node-size" value="type"/>Type</label></li>
                  <li><label class="radio"><input type="radio" name="node-size" value="page-rank"/>Page rank [importance]</label></li>
                  <li><label class="radio"><input type="radio" name="node-size" value="dist-auth"/>Contributors</label></li>
                </ul>
              </li>
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Node colour<b class="caret"></b></a>
                <ul class="dropdown-menu">
                  <li><label class="radio"><input type="radio" name="node-color" value="black" checked="checked"/>Black</label></li>
                  <li><label class="radio"><input type="radio" name="node-color" value="type" checked="checked"/>Type</label></li>
                  <li><label class="radio"><input type="radio" name="node-color" value="by-avg-loc"/>Avg. lines of code / class [quality]</label></li>
                  <li><label class="radio"><input type="radio" name="node-color" value="author" checked="checked"/>Owner</label></li>
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </nav>
      """)
    onCheckLinkType = (selectorOrElement, attr) ->
      $element = selectorOrElement
      if ((typeof selectorOrElement) == "string")
        $element = $(selectorOrElement)
      if ($element.is(":checked"))
        linkColors[attr] = defaultLinkColors[attr]
        strengths[attr] = defaultStrengths[attr]
        linkWidths[attr] = defaultLinkWidths[attr]
      else
        linkColors[attr] = () -> "transparent"
        strengths[attr] = () -> 0
        linkWidths[attr] = () -> 0
      link
        .transition()
          .duration(750)
          .style("stroke", linkColor)
          .style("stroke-width", linkWidth)
      force.linkStrength(strength).start()
    onCheckLinkType(".check-pkg-imports", "pkg-imports-pkg")
    onCheckLinkType(".check-imports", "cls-imports-cls")
    onCheckLinkType(".check-contains", "in-package")
    onCheckLinkType(".check-runtime", "calls")

    checkLinkType = (selector, attr) ->
      $elem = $(selector)
      $elem.on "click", ->
        onCheckLinkType($elem, attr)
    checkLinkType(".check-pkg-imports", "pkg-imports-pkg")
    checkLinkType(".check-imports", "cls-imports-cls")
    checkLinkType(".check-contains", "in-package")
    checkLinkType(".check-runtime", "calls")
    onCheckColourType = (selectorOrElement) ->
      $element = selectorOrElement
      if ((typeof selectorOrElement) == "string")
        $element = $(selectorOrElement)
      if (!$element || ($element.is(":checked") and $element.attr("value") == "black"))
        color = kindNodeColor
      else
        color = avgLocColor
      node
        .transition()
          .duration(750)
          .style("fill", color)
    onCheckColourType("""input[name="node-color"]:checked""")

    $("""input[name="node-color"]""").on "click", ->
      onCheckColourType($(this))

    onCheckNodeSize = (selectorOrElement) ->
      $element = selectorOrElement
      if ((typeof selectorOrElement) == "string")
        $element = $(selectorOrElement)
      if (!$element || $element.is(":checked") and $element.attr("value") == "constant")
        nodeR = kindNodeSize
      else
        nodeR = pageRankNodeSize
      node
        .transition()
          .duration(750)
          .attr("r", nodeSize)

    onCheckNodeSize("""input[name="node-size"]:checked""")
    $("""input[name="node-size"]""").on "click", ->
      onCheckNodeSize($(this))
    currentCreateFunction = (opt_sha) ->
      makeSvg(jsonAddress("custom.json", opt_sha))
    currentCreateFunction()

  $(".packages-button").on "click", (event) -> if (!$(".nav-graph-packages-tab").hasClass("active"))
    defaultMetrics()
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-packages-tab").addClass("active")
    $(".gauges").remove()
    currentCreateFunction = (opt_sha) ->
      makeSvg(jsonAddress("packages.json", opt_sha))
    currentCreateFunction()
    $("[rel='tooltip']").tooltip()

  $(".package-imports-button").on "click", (event) -> if (!$(".nav-graph-package-imports-tab").hasClass("active"))
    defaultMetrics()
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-package-imports-tab").addClass("active")
    $(".gauges").remove()
    currentCreateFunction = (opt_sha) ->
     makeSvg(jsonAddress("pkgImports.json", opt_sha))
    currentCreateFunction()
    $("[rel='tooltip']").tooltip()

  $(".class-imports-button").on "click", (event) -> if (!$(".nav-graph-class-imports-tab").hasClass("active"))
    defaultMetrics()
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-class-imports-tab").addClass("active")
    $(".gauges").remove()
    currentCreateFunction = (opt_sha) ->
      makeSvg(jsonAddress("clsImports.json", opt_sha))
    currentCreateFunction()
    $("[rel='tooltip']").tooltip()

  $(".full-button").on "click", (event) -> if (!$(".nav-graph-full-tab").hasClass("active"))
    defaultMetrics()
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-full-tab").addClass("active")
    $(".gauges").remove()
    currentCreateFunction = (opt_sha) ->
      makeSvg(jsonAddress("full.json", opt_sha))
    currentCreateFunction()
    $("[rel='tooltip']").tooltip()

  currentCreateFunction()
  $("[rel='tooltip']").tooltip()
  $("#revGraph").on "revchange", (event, sha) ->
    currentCreateFunction(sha)

