package magnify.services.metrics

import magnify.features.Metric
import magnify.model.graph.FullGraph
import play.api.Logger

class ExperienceMetric extends Metric {

  val logger = Logger(classOf[ExperienceMetric].getSimpleName)

  override def apply(g: FullGraph): FullGraph = g
}
