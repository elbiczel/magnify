package magnify.services.metrics

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

import com.google.inject.name.Named
import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.{LoggedFunction, RevisionMetric}
import magnify.model.graph.{AsVertex, FullGraph, Graph}
import play.api.Logger

abstract class AggregatedContributionMetric
    extends AbstractRevWalkMetric[Map[String, Double]](AggregateContributionTransformation) {

  final val logger = Logger(classOf[AggregatedContributionMetric].getSimpleName)

  override def aggregateMetric(g: FullGraph): FullGraph = {
    g.vertices.has("kind", "commit").transform(new AsVertex)
        .sideEffect(new AggregateAggrContributionExperience(Direction.IN, "in-revision", "class"))
        .iterate()
    g
  }
}

class LoggedAggregatedContributionMetric(@Named("ServicesPool") implicit override val pool: ExecutionContext)
    extends AggregatedContributionMetric with LoggedFunction[FullGraph, FullGraph]

class RevisionAggregatedContributionMetric extends RevisionMetric {

  final val logger = Logger(classOf[RevisionAggregatedContributionMetric].getSimpleName)

  override def apply(graph: Graph): Graph = {
    graph.vertices.has("kind", "package").transform(new AsVertex)
        .sideEffect(new AggregateAggrContributionExperience(Direction.IN, "in-package", "class"))
        .iterate()
    graph
  }
}

class LoggedRevisionAggregatedContributionMetric
    extends RevisionAggregatedContributionMetric with LoggedFunction[Graph, Graph]

private[this] object AggregateContributionTransformation extends RevisionTransformation[Map[String, Double]] {

  override def metricName: String = "aggr-contribution"

  override def metric(revision: Vertex, current: Vertex, oParent: Option[Vertex]): Map[String, Double] = {
    val authorId = getName(revision)
    val prevExperience: Map[String, Double] = oParent
        .map(getMetricValue[Map[String, Double]](metricName, _))
        .getOrElse(Map[String, Double]())
    val newAuthorExperience = prevExperience.getOrElse(authorId, 0.0) + getMetricValue[Double]("contribution", current)
    val updatedExperience = prevExperience.updated(authorId, newAuthorExperience)
    updatedExperience
  }
}

private[this] class AggregateAggrContributionExperience(dir: Direction, label: String, kind: String)
  extends AggregatingMetricTransformation[Map[String, Double]](dir, label, kind, "aggr-contribution") {

  override def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Map[String, Double] = {
    val individuals = pipe.toList.toSeq.map(getMetricValue[Map[String, Double]](metricName, _))
    individuals.foldLeft(Map[String, Double]()) { case (reduced, singleContribution) =>
      (reduced.keySet ++ singleContribution.keySet).map { (key) =>
        val newValue = reduced.getOrElse(key, 0.0) + singleContribution.getOrElse(key, 0.0)
        (key -> newValue)
      }.toMap
    }
  }
}
