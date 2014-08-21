package magnify.features.view

import java.lang
import java.text.SimpleDateFormat
import java.util.Date

import com.tinkerpop.blueprints.Vertex
import magnify.features.{AuthorId, MetricNames}

trait ElementView extends AuthorId {

  final protected val format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

  final protected def timestamp(vrtx: Vertex): String =
    format.format(new Date(vrtx.getProperty[Integer]("time") * 1000L))

  final protected def additionalRevData(vrtx: Vertex): Map[String, String] = {
    val aggregatedContribution = vrtx.getProperty[Map[String, Double]](
      MetricNames.propertyName(MetricNames.aggregatedContribution)).map { case (author, contribution) =>
      (MetricNames.propertyName(MetricNames.aggregatedContribution) + "---" + author -> contribution.toString)
    }
    val experience = vrtx.getProperty[Map[String, Double]](MetricNames.propertyName(MetricNames.experience))
        .map { case (author, exp) =>
      (MetricNames.propertyName(MetricNames.experience) + "---" + author -> exp.toString)
    }
    val contribution = vrtx.getProperty[Double](MetricNames.propertyName(MetricNames.contribution))
    val complexity = vrtx.getProperty[Double](MetricNames.propertyName(MetricNames.mcCabeCyclomaticComplexity))
    val loc = vrtx.getProperty[Double](MetricNames.propertyName(MetricNames.linesOfCode))
    val avgLoc = vrtx.getProperty[Double](MetricNames.propertyName(MetricNames.averageLinesOfCode))
    val authors = vrtx.getProperty[lang.Integer](MetricNames.propertyName(MetricNames.distinctAuthors))
    aggregatedContribution ++ experience ++ Map(
      MetricNames.propertyName(MetricNames.contribution) -> contribution.toString,
      MetricNames.propertyName(MetricNames.mcCabeCyclomaticComplexity) -> complexity.toString,
      MetricNames.propertyName(MetricNames.linesOfCode) -> loc.toString,
      MetricNames.propertyName(MetricNames.averageLinesOfCode) -> avgLoc.toString,
      MetricNames.propertyName(MetricNames.distinctAuthors) -> authors.toString)
  }

  final protected def pageRankMap(vrtx: Vertex): Map[String, String] = {
    val pageRank = vrtx.getProperty[String](MetricNames.propertyName(MetricNames.pageRank))
    Map(MetricNames.propertyName(MetricNames.pageRank) -> pageRank)
  }
}
