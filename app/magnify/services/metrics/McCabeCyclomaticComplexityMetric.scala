package magnify.services.metrics

import scala.collection.JavaConversions._

import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.{FullGraphMetric, LoggedFunction, MetricNames, RevisionMetric}
import magnify.model.graph.{Actions, AsVertex, FullGraph, Graph}
import play.api.Logger

class McCabeCyclomaticComplexityMetric extends FullGraphMetric with Actions {

  val logger = Logger(classOf[McCabeCyclomaticComplexityMetric].getSimpleName)

  override def apply(graph: FullGraph): FullGraph = {
    graph.vertices.has("kind", "commit").transform(new AsVertex)
        .sideEffect(new AggregateMcCabeComplexityMetric(Direction.IN, "in-revision", "class"))
        .iterate()
    graph
  }

  override final val name: String = MetricNames.mcCabeCyclomaticComplexity

  override final val dependencies: Set[String] = Set(MetricNames.linesOfCode)
}

class LoggedMcCabeCyclomaticComplexityMetric
    extends McCabeCyclomaticComplexityMetric with LoggedFunction[FullGraph, FullGraph]


class RevisionMcCabeCyclomaticComplexityMetric extends RevisionMetric {

  val logger = Logger(classOf[RevisionMcCabeCyclomaticComplexityMetric].getSimpleName)

  override def apply(graph: Graph): Graph = {
    graph.vertices.has("kind", "package").transform(new AsVertex)
        .sideEffect(new AggregateMcCabeComplexityMetric(Direction.IN, "cls-in-pkg", "class"))
        .iterate()
    graph
  }

  override final val name: String = MetricNames.mcCabeCyclomaticComplexity

  override final val dependencies: Set[String] = Set(MetricNames.linesOfCode, MetricNames.pageRank)
}

class LoggedRevisionMcCabeCyclomaticComplexityMetric
    extends RevisionMcCabeCyclomaticComplexityMetric with LoggedFunction[Graph, Graph]

private[this] class AggregateMcCabeComplexityMetric(dir: Direction, label: String, kind: String)
    extends AggregatingMetricTransformation[Double](
      dir, label, kind, MetricNames.mcCabeCyclomaticComplexity) {

  override def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Double = {
    val classes = pipe.toList.toSeq
    if (classes.isEmpty) { 0.0 } else {
      val mcCabeComplexities = classes.map(getMetricValue[Double](metricName, _))
      if (mcCabeComplexities.sum == 0.0) { 0.0 } else {
        val weights = getWeights(classes)
        val weightedComplexities = mcCabeComplexities.zip(weights).map {case (cc, weight) => cc * weight}
        weightedComplexities.sum / weights.sum
      }
    }
  }

  private def getWeights(classes: Seq[Vertex]): Seq[Double] =
    if (classes.forall(_.getPropertyKeys.contains(MetricNames.propertyName(MetricNames.pageRank)))) {
      classes.map(_.getProperty[String](MetricNames.propertyName(MetricNames.pageRank)).toDouble)
    } else {
      classes.map(getMetricValue[Double](MetricNames.linesOfCode, _))
    }
}
