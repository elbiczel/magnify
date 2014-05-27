package magnify.services.metrics

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.{LoggedFunction, MetricNames, RevisionMetric}
import magnify.model.graph.{Actions, AsVertex, Graph}
import play.api.Logger

class RevisionAvgLocMetric extends RevisionMetric with Actions {

  val logger = Logger(classOf[RevisionAvgLocMetric].getSimpleName)

  override def apply(g: Graph): Graph = {
    g.vertices.has("kind", "package").transform(new AsVertex).sideEffect(
      new AggregatingMetricTransformation[Double](Direction.IN, "cls-in-pkg", "class", name) {
        override def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Double = {
          val classMetrics = pipe.toList.toSeq.map(getMetricValue[Double](MetricNames.linesOfCode, _))
          Option(classMetrics).filter(_.size > 0).map(_.sum / classMetrics.size).getOrElse(0.0)
        }
      }).iterate()
    g
  }

  override final val name: String = MetricNames.averageLinesOfCode

  override final val dependencies: Set[String] = Set(MetricNames.linesOfCode)
}

class LoggedRevisionAvgLocMetric extends RevisionAvgLocMetric with LoggedFunction[(Graph, Vertex), Graph]
