package magnify.services.metrics

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.{LoggedFunction, Metric, RevisionMetric}
import magnify.model.graph.{Actions, AsVertex, FullGraph, Graph}
import play.api.Logger

class McCabeCyclomaticComplexityMetric extends Metric with Actions {

  val logger = Logger(classOf[McCabeCyclomaticComplexityMetric].getSimpleName)

  override def apply(graph: FullGraph): FullGraph = {
    graph.vertices.has("kind", "commit").transform(new AsVertex)
        .sideEffect(new AggregateMcCabeComplexityMetric(Direction.IN, "in-revision", "class"))
        .iterate()
    graph
  }
}

class LoggedMcCabeCyclomaticComplexityMetric
    extends McCabeCyclomaticComplexityMetric with LoggedFunction[FullGraph, FullGraph]


class RevisionMcCabeCyclomaticComplexityMetric extends RevisionMetric {

  val logger = Logger(classOf[RevisionMcCabeCyclomaticComplexityMetric].getSimpleName)

  override def apply(graph: Graph): Graph = {
    graph.vertices.has("kind", "package").transform(new AsVertex)
        .sideEffect(new AggregateMcCabeComplexityMetric(Direction.IN, "in-package", "class"))
        .iterate()
    graph
  }
}

class LoggedRevisionMcCabeCyclomaticComplexityMetric
    extends RevisionMcCabeCyclomaticComplexityMetric with LoggedFunction[Graph, Graph]

private[this] class AggregateMcCabeComplexityMetric(dir: Direction, label: String, kind: String)
    extends AggregatingMetricTransformation[Double](dir, label, kind, "mcCabeCC") {

  override def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Double = {
    val classes = pipe.toList.toSeq
    val mcCabeComplexities = classes.map(getMetricValue[Double](metricName, _))
    val weights = getWeights(classes)
    val weightedComplexities = mcCabeComplexities.zip(weights).map { case (cc, weight) => cc * weight }
    weightedComplexities.sum / weights.sum
  }

  private def getWeights(classes: Seq[Vertex]): Seq[Double] =
    if (classes.forall(_.getPropertyKeys.contains("page-rank"))) {
      classes.map(_.getProperty[String]("page-rank").toDouble)
    } else {
      classes.map(getMetricValue[Double]("lines-of-code", _))
    }
}
