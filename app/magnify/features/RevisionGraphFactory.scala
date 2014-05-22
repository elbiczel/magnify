package magnify.features

import magnify.model.graph.{FullGraph, Graph}

trait RevisionGraphFactory extends ((FullGraph, Option[String]) => Graph) {

  final def apply(graph: FullGraph): Graph = apply(graph, None)
}
