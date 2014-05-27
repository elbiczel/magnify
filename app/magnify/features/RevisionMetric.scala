package magnify.features

import com.tinkerpop.blueprints.Vertex
import magnify.model.graph.Graph

trait RevisionMetric extends Metric[(Graph, Vertex), Graph] {

  override def apply(p: (Graph, Vertex)): Graph = apply(p._1)

  protected def apply(g: Graph): Graph
}
