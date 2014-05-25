$ ->
  currentCreateFunction = ->
    makeSvg(jsonAddress("packages.json"))
  jsonAddress = (jsonAddress) ->
    jsonAddress + "?rev=" + getActiveSha();

  badness = d3.scale.linear().domain([-1, 300]).range(["green", "red"])

  defaultNodeColor =
    "package": "#000"
    "class": "#555"
  kindNodeColor = (node) -> defaultNodeColor[node.kind]
  metricNodeColor = (metric) -> (node) -> badness(node["metric--" + metric])
  avgLocColor = metricNodeColor("avg-loc")
  color = null

  defaultStrengths =
    "in-package": 1.0 # 0.5
    "package-imports": 0.03 # 0.1
    imports: 0.01
    calls: 0.01
  strengths = null
  strength = (link) -> strengths[link.kind]

  defaultLinkColors =
    "in-package": "#cc0000"
    "package-imports": "#babdb6"
    imports: "#d3d7df"
    calls: "#fce94f"
  linkColors = null
  linkColor = (link) -> linkColors[link.kind]

  defaultLinkWidths =
    "in-package": () -> 1.5
    "package-imports": () -> 1
    imports: () -> 1
    calls: (link) -> Math.min(link.count / 10.0, 5)
  linkWidths = null
  linkWidth = (link) -> linkWidths[link.kind](link)


  defaultNodeSize =
    "package": 5
    "class": 1
  kindNodeSize = (node) -> defaultNodeSize[node.kind]
  metricNodeSize = (metric) -> (node) -> 3 + Math.max(3, 100.0 * node["metric--" + metric])
  pageRankNodeSize = metricNodeSize("pr")
  nodeR = null

  defaultMetrics = () ->
    color = avgLocColor
    strengths =
      "in-package": defaultStrengths["in-package"]
      "package-imports": defaultStrengths["package-imports"]
      imports: defaultStrengths.imports
      calls: defaultStrengths.calls
    linkColors =
      "in-package": defaultLinkColors["in-package"]
      "package-imports": defaultLinkColors["package-imports"]
      imports: defaultLinkColors.imports
      calls: defaultLinkColors.calls
    linkWidths =
      "in-package": defaultLinkWidths["in-package"]
      "package-imports": defaultLinkWidths["package-imports"]
      imports: defaultLinkWidths.imports
      calls: defaultLinkWidths.calls
    nodeR = pageRankNodeSize
  defaultMetrics()

  width = $("#chart").width()
  height = $("#chart").height()

  force = d3.layout.force()
    .charge(-120)
    .linkDistance(30)
    .linkStrength(strength)
    .size([width, height])
    .gravity(0.2)

  svg = d3
    .select("#chart")
    .append("svg:svg")
    .attr("width", width)
    .attr("height", height)
    .attr("pointer-events", "all")
    .append("svg:g")
    .call(d3.behavior.zoom().on("zoom", ->
        svg.attr("transform", "translate(#{d3.event.translate}) scale(#{d3.event.scale})")
    ))
    .append("svg:g")

  svg
    .append("svg:rect")
    .attr("width", width)
    .attr("height", height)
    .attr("fill", "transparent")
    .attr("pointer-events", "all")

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

  link = svg.selectAll("line.link")
  node = svg.selectAll("circle.node")

  makeSvg = (jsonAddress) ->
    d3.json jsonAddress, (json) ->
      force
        .nodes(json.nodes)
        .links(json.edges)
        .start()

      link = svg.selectAll("line.link")
        .data(json.edges, (d) -> d.source.index + "," + d.target.index)

      # update
      #EMPTY

      # enter
      enterLink = link.enter()
        .append("svg:line")
        .attr("class", "link")
        .attr("marker-end", "url(#arrow-head)")
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

      node = svg.selectAll("circle.node")
        .data(json.nodes, (d) -> d.name)

      # update
      #EMPTY

      # enter
      enterNode = node.enter()
        .append("circle")
        .attr("class", "node")
        .attr("r", 1e-6)
        .style("fill-opacity", 1e-6)
        .call(force.drag)
      enterNode
        .append("title")
        .text((d) -> d.name)

      # enter + update
      node
        .style("fill", color)
        .transition()
          .duration(750)
          .attr("r", nodeR)
          .style("fill-opacity", 1)


      # exit
      exitNode = node.exit()
        .attr("class", "exit")
        .transition(750)
          .style("fill-opacity", 1e-6)
          .remove()

      force.on "tick", ->
        link
          .attr("x1", (d) -> d.source.x)
          .attr("y1", (d) -> d.source.y)
          .attr("x2", (d) -> d.target.x)
          .attr("y2", (d) -> d.target.y)
        node
          .attr("cx", (d) -> d.x)
          .attr("cy", (d) -> d.y)

  $(".custom-button").on "click", (event) -> if (!$(".nav-graph-custom-tab").hasClass("active"))
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-custom-tab").addClass("active")
    $(".gauges").remove()
    $(".mag-sidenav").after(
      """
      <nav class="navbar navbar-default gauges" role="navigation">
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
                  <li><label class="radio"><input type="radio" name="node-size" value="page-rank"/>Page rank</label></li>
                </ul>
              </li>
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Node colour<b class="caret"></b></a>
                <ul class="dropdown-menu">
                  <li><label class="radio"><input type="radio" name="node-color" value="black" checked="checked"/>Black</label></li>
                  <li><label class="radio"><input type="radio" name="node-color" value="by-avg-loc"/>Avg. lines of code / class</label></li>
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
        linkColors[attr] = "transparent"
        strengths[attr] = 0
        linkWidths[attr] = () -> 0
      link
        .transition()
          .duration(750)
          .style("stroke", linkColor)
          .style("stroke-width", linkWidth)
      force.linkStrength(strength).start()
    onCheckLinkType(".check-pkg-imports", "package-imports")
    onCheckLinkType(".check-imports", "imports")
    onCheckLinkType(".check-contains", "in-package")
    onCheckLinkType(".check-runtime", "calls")

    checkLinkType = (selector, attr) ->
      $elem = $(selector)
      $elem.on "click", ->
        onCheckLinkType($elem, attr)
    checkLinkType(".check-pkg-imports", "package-imports")
    checkLinkType(".check-imports", "imports")
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
          .attr("r", nodeR)

    onCheckNodeSize("""input[name="node-size"]:checked""")
    $("""input[name="node-size"]""").on "click", ->
      onCheckNodeSize($(this))
    currentCreateFunction = ->
      makeSvg(jsonAddress("custom.json"))
    currentCreateFunction()

  $(".packages-button").on "click", (event) -> if (!$(".nav-graph-packages-tab").hasClass("active"))
    defaultMetrics()
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-packages-tab").addClass("active")
    $(".gauges").remove()
    currentCreateFunction = ->
      makeSvg(jsonAddress("packages.json"))
    currentCreateFunction()
    $("[rel='tooltip']").tooltip()

  $(".package-imports-button").on "click", (event) -> if (!$(".nav-graph-package-imports-tab").hasClass("active"))
    defaultMetrics()
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-package-imports-tab").addClass("active")
    $(".gauges").remove()
    currentCreateFunction = ->
     makeSvg(jsonAddress("pkgImports.json"))
    currentCreateFunction()
    $("[rel='tooltip']").tooltip()

  $(".class-imports-button").on "click", (event) -> if (!$(".nav-graph-class-imports-tab").hasClass("active"))
    defaultMetrics()
    $(".nav-graph-detail-level").find("*").removeClass("active")
    $(".nav-graph-class-imports-tab").addClass("active")
    $(".gauges").remove()
    currentCreateFunction = ->
      makeSvg(jsonAddress("clsImports.json"))
    currentCreateFunction()
    $("[rel='tooltip']").tooltip()

  currentCreateFunction()
  $("[rel='tooltip']").tooltip()
  $("#revisions").on "revchange", (event) ->
    currentCreateFunction()

