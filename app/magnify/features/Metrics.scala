package magnify.features

import magnify.model.graph.FullGraph

trait Metrics {
  def apply(g: FullGraph): FullGraph
}
