package magnify.features

import magnify.model.graph.FullGraph
import play.api.Logger

trait Metric extends (FullGraph => FullGraph)

trait LoggedMetric extends (FullGraph => FullGraph) {

  protected val logger: Logger

  abstract override def apply(g: FullGraph): FullGraph = {
    logger.info { "starting: " + System.nanoTime() }
    val result = super.apply(g)
    logger.info { "finished: " + System.nanoTime() }
    result
  }
}
