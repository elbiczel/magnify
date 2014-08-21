package magnify.features.view

import java.lang
import java.text.SimpleDateFormat
import java.util.Date

import com.tinkerpop.blueprints.Vertex
import magnify.features.AuthorId

trait ElementView extends AuthorId {

  import magnify.features.MetricNames._

  final protected val format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

  final protected def timestamp(vrtx: Vertex): String =
    format.format(new Date(vrtx.getProperty[Integer]("time") * 1000L))

  final protected def additionalRevData(vrtx: Vertex): Map[String, String] = {
    val aggregatedContributionValues = vrtx.getProperty[Map[String, Double]](
      propertyName(aggregatedContribution)).map { case (author, contribution) =>
      (propertyName(aggregatedContribution) + "---" + author -> contribution.toString)
    }
    val experienceValues = vrtx.getProperty[Map[String, Double]](propertyName(experience))
        .map { case (author, exp) =>
      (propertyName(experience) + "---" + author -> exp.toString)
    }
    val contributionValue = vrtx.getProperty[Double](propertyName(contribution))
    val complexityValue = vrtx.getProperty[Double](propertyName(mcCabeCyclomaticComplexity))
    val locValue = vrtx.getProperty[Double](propertyName(linesOfCode))
    val avgLocValue = vrtx.getProperty[Double](propertyName(averageLinesOfCode))
    val authorsValue = vrtx.getProperty[lang.Integer](propertyName(distinctAuthors))
    val ownerValue = vrtx.getProperty[String](propertyName(owner))
    aggregatedContributionValues ++ experienceValues ++ Map(
      propertyName(contribution) -> contributionValue.toString,
      propertyName(mcCabeCyclomaticComplexity) -> complexityValue.toString,
      propertyName(linesOfCode) -> locValue.toString,
      propertyName(averageLinesOfCode) -> avgLocValue.toString,
      propertyName(distinctAuthors) -> authorsValue.toString,
      propertyName(owner) -> ownerValue.toString)
  }

  final protected def pageRankMap(vrtx: Vertex): Map[String, String] = {
    val pageRankValue = vrtx.getProperty[String](propertyName(pageRank))
    Map(propertyName(pageRank) -> pageRankValue)
  }
}
