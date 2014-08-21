package magnify.features

import com.tinkerpop.blueprints.Vertex
import magnify.model.graph.Graph

trait RevisionMetric extends Metric[(Graph, Vertex), Graph] {

  // This should never be overridden. Can't make it final due to Scala trait magic.
  override def apply(p: (Graph, Vertex)): Graph = apply(p._1)

  protected def apply(g: Graph): Graph
}
