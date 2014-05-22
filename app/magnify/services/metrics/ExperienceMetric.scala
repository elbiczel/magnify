package magnify.services.metrics


import scala.concurrent.ExecutionContext

import com.google.inject.name.Named
import com.tinkerpop.blueprints.Vertex
import magnify.features.LoggedMetric
import play.api.Logger

abstract class ExperienceMetric extends AbstractRevWalkMetric[Map[String, Long]] {

  final val logger = Logger(classOf[ExperienceMetric].getSimpleName)

  override final val transformation: RevisionTransformation[Map[String, Long]] = ExperienceTransformation
}

class LoggedExperienceMetric(
    @Named("ServicesPool") implicit override val pool: ExecutionContext)
  extends ExperienceMetric with LoggedMetric

private[this] object ExperienceTransformation extends RevisionTransformation[Map[String, Long]] {
  override def metric(revision: Vertex, current: Vertex, oParent: Option[Vertex]): Map[String, Long] = {
    val authorId = getName(revision.getProperty[String]("author"))
    val prevExperience: Map[String, Long] = oParent
        .map(_.getProperty[Map[String, Long]]("metric--exp"))
        .getOrElse(Map())
    val newAuthorExperience = prevExperience.getOrElse(authorId, 0L) + 5
    val baseUpdatedExperience = prevExperience.updated(authorId, newAuthorExperience)
    val otherAuthors = prevExperience.keys.filter(_ != authorId)
    val updatedExperienceWithPenalty = otherAuthors.foldLeft(baseUpdatedExperience) { case (expMap, authorId) =>
      expMap.updated(authorId, expMap(authorId) - 1)
    }
    updatedExperienceWithPenalty
  }

  override def metricName: String = "exp"
}
