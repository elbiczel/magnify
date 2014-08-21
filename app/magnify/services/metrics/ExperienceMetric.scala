package magnify.services.metrics

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

import com.google.inject.name.Named
import com.tinkerpop.blueprints.{Direction, Edge, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.{AuthorId, LoggedFunction, MetricNames, RevisionMetric}
import magnify.model.graph.{AsVertex, FullGraph, Graph}
import play.api.Logger

abstract class ExperienceMetric extends AbstractRevWalkMetric[Map[String, Double]](ExperienceTransformation) {

  final val logger = Logger(classOf[ExperienceMetric].getSimpleName)

  override def aggregateMetric(g: FullGraph): FullGraph = {
    g.vertices.has("kind", "commit").transform(new AsVertex)
        .sideEffect(new GetAggregatedExperience(Direction.IN, "in-revision", "class"))
        .iterate()
    g
  }

  override final val name: String = MetricNames.experience

  override final val dependencies: Set[String] = Set(MetricNames.linesOfCode)

  override final val isSerializable: Boolean = false
}

class LoggedExperienceMetric(@Named("ServicesPool") implicit override val pool: ExecutionContext)
  extends ExperienceMetric with LoggedFunction[FullGraph, FullGraph]

class RevisionExperienceMetric extends RevisionMetric {

  final val logger = Logger(classOf[RevisionExperienceMetric].getSimpleName)

  override def apply(graph: Graph): Graph = {
    graph.vertices.has("kind", "package").transform(new AsVertex)
          .sideEffect(new GetAggregatedExperience(Direction.IN, "cls-in-pkg", "class"))
          .iterate()
    graph
  }

  override final val name: String = MetricNames.experience

  override final val dependencies: Set[String] = Set(MetricNames.linesOfCode)

  override final val isSerializable: Boolean = false
}

class LoggedRevisionExperienceMetric extends RevisionExperienceMetric with LoggedFunction[(Graph, Vertex), Graph]

private[this] class GetAggregatedExperience(dir: Direction, label: String, kind: String)
    extends AggregatingMetricTransformation[Map[String, Double]](dir, label, kind, MetricNames.experience) {

  override final def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Map[String, Double] = {
    val individuals = pipe.toList.toSeq
    val weightMetricPairs = individuals.map((v) => (
        getMetricValue[Double](MetricNames.linesOfCode, v) -> getMetricValue[Map[String, Double]](metricName, v)))
    val weightSum = weightMetricPairs.map(_._1).sum
    val weightedExps = weightMetricPairs.map { case (weight, singleExp) =>
      singleExp.mapValues(_ * weight)
    }
    weightedExps.foldLeft(Map[String, Double]()) { case (reduced, singleExp) =>
      (reduced.keySet ++ singleExp.keySet).map { (key) =>
        val newValue = reduced.getOrElse(key, 0.0) + singleExp.getOrElse(key, 0.0)
        (key -> newValue)
      }.toMap
    }.mapValues(_ / weightSum)
  }
}

private[this] object ExperienceTransformation extends RevisionTransformation[Map[String, Double]] with AuthorId {
  override def metric(
      revision: Vertex,
      current: Vertex,
      oParent: Option[Vertex],
      oCommit: Option[Edge]): Map[String, Double] = {
    val authorId = getName(revision)
    val prevExperience: Map[String, Double] = oParent
        .map(getMetricValue[Map[String, Double]](metricName, _))
        .getOrElse(Map[String, Double]())
    val newAuthorExperience = prevExperience.getOrElse(authorId, 0.0) + 5.0
    val baseUpdatedExperience = prevExperience.updated(authorId, newAuthorExperience)
    val otherAuthors = prevExperience.keys.filter(_ != authorId)
    val updatedExperienceWithPenalty = otherAuthors.foldLeft(baseUpdatedExperience) { case (expMap, authorId) =>
      expMap.updated(authorId, expMap(authorId) - 1.0)
    }
    updatedExperienceWithPenalty
  }
}
