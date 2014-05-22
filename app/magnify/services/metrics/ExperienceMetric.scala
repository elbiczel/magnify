package magnify.services.metrics

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

import com.google.inject.name.Named
import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import magnify.features.{LoggedFunction, RevisionMetric}
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
}

class LoggedExperienceMetric(@Named("ServicesPool") implicit override val pool: ExecutionContext)
  extends ExperienceMetric with LoggedFunction[FullGraph, FullGraph]

class RevisionExperienceMetric extends RevisionMetric {

  final val logger = Logger(classOf[RevisionExperienceMetric].getSimpleName)

  override def apply(graph: Graph): Graph = {
    graph.vertices.has("kind", "package").transform(new AsVertex)
          .sideEffect(new GetAggregatedExperience(Direction.IN, "in-package", "class"))
          .iterate()
    graph
  }
}

class LoggedRevisionExperienceMetric extends RevisionExperienceMetric with LoggedFunction[Graph, Graph]

private[this] class GetAggregatedExperience(dir: Direction, label: String, kind: String)
    extends AggregatingMetricTransformation[Map[String, Double]](dir, label, kind, "exp") {

  override final def metricValue(pipe: GremlinPipeline[Vertex, Vertex]): Map[String, Double] = {
    val individuals = pipe.toList.toSeq
    val weightMetricPairs = individuals.map((v) => (
        v.getProperty[Double]("metric--lines-of-code") -> v
            .getProperty[Map[String, Double]]("metric--exp")))
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

private[this] object ExperienceTransformation extends RevisionTransformation[Map[String, Double]] {
  override def metric(revision: Vertex, current: Vertex, oParent: Option[Vertex]): Map[String, Double] = {
    val authorId = getName(revision)
    val prevExperience: Map[String, Double] = oParent
        .map(_.getProperty[Map[String, Double]]("metric--exp"))
        .getOrElse(Map())
    val newAuthorExperience = prevExperience.getOrElse(authorId, 0.0) + 5.0
    val baseUpdatedExperience = prevExperience.updated(authorId, newAuthorExperience)
    val otherAuthors = prevExperience.keys.filter(_ != authorId)
    val updatedExperienceWithPenalty = otherAuthors.foldLeft(baseUpdatedExperience) { case (expMap, authorId) =>
      expMap.updated(authorId, expMap(authorId) - 1.0)
    }
    updatedExperienceWithPenalty
  }

  override def metricName: String = "exp"
}
