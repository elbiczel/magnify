package magnify.features

import magnify.model.graph.FullGraph

trait Metric extends (FullGraph => FullGraph)
